import { messageFor, runBatch } from "./worker.ts";
import type { OutboxJob, WorkerPorts, Delivery } from "./worker.ts";
import { sendFcm } from "../_shared/fcm.ts";

function assert(condition: unknown, message = "Assertion failed"): asserts condition {
  if (!condition) throw Error(message);
}
const now = Date.parse("2026-09-30T12:00:00Z");
function job(): OutboxJob {
  return { outbox_id: "outbox", request_id: "request", doctor_id: "doctor", doctor_user_id: "auth",
    event_type: "consultation_invite", request_version: 1, attempts: 1,
    payload: { expires_at: new Date(now + 60000).toISOString() },
    device_tokens: [{ token: "one", installation_id: "phone1", platform: "android" },
      { token: "two", installation_id: "phone2", platform: "android" }] };
}
function fake(row: OutboxJob, results: Delivery[] = [{ status: "sent" }, { status: "sent" }]) {
  const sent: string[] = [], failed: string[] = [], removed: string[] = [], deliveries: string[] = [];
  const ports: WorkerPorts = {
    claim: async () => [row], sent: async id => { sent.push(id); },
    failed: async (_, reason) => { failed.push(reason); },
    removeToken: async (owner, installation, token) => { removed.push([owner, installation, token].join("/")); },
    deliver: async token => { deliveries.push(token); return results.shift() ?? { status: "failed" }; },
  };
  return { ports, sent, failed, removed, deliveries };
}
Deno.test("successful invitation reaches every registered Android device", async () => {
  const f = fake(job()); const result = await runBatch(f.ports, () => now);
  assert(result.sent === 1 && f.deliveries.join() === "one,two" && f.failed.length === 0);
});
Deno.test("partial device failure retries row without acknowledging complete delivery", async () => {
  const f = fake(job(), [{ status: "sent" }, { status: "failed" }]);
  await runBatch(f.ports, () => now); assert(f.sent.length === 0 && f.failed.length === 1);
});
Deno.test("only definitive unregistered tokens are removed with exact ownership", async () => {
  const f = fake(job(), [{ status: "unregistered" }, { status: "sent" }]);
  await runBatch(f.ports, () => now);
  assert(f.removed.join() === "auth/phone1/one" && f.sent.length === 1);
});
Deno.test("expired invitation is discarded without a push", async () => {
  const f = fake(job()); const result = await runBatch(f.ports, () => now + 60000);
  assert(result.expired === 1 && f.deliveries.length === 0 && f.sent.length === 1);
});
Deno.test("no device is a retryable delivery failure", async () => {
  const row = job(); row.device_tokens = []; const f = fake(row);
  await runBatch(f.ports, () => now); assert(f.failed.length === 1 && f.sent.length === 0);
});
Deno.test("close event reaches devices even after invitation expiry", async () => {
  const row = job(); row.event_type = "consultation_closed"; row.request_version = 2;
  const message = messageFor(row, now + 600000);
  assert(message?.ttl === 300 && message.data.version === "2" && !('expires_at' in message.data));
  const f = fake(row); await runBatch(f.ports, () => now + 600000); assert(f.sent.length === 1);
});
Deno.test("payload allowlist excludes outbox personal information", () => {
  const row = job(); Object.assign(row.payload, { patient_name: "private", token: "secret" });
  const data = messageFor(row, now)!.data;
  assert(Object.keys(data).sort().join() === "expires_at,recipient_user_id,request_id,type,version");
});
Deno.test("malformed expiry and versions never dispatch", async () => {
  for (const change of [{ request_version: 0 }, { payload: { expires_at: "bad" } }, { event_type: "unknown" }]) {
    const f = fake(Object.assign(job(), change)); await runBatch(f.ports, () => now);
    assert(f.deliveries.length === 0 && f.failed.length === 1);
  }
});
Deno.test("worker budget stops further sends before lease reclaim", async () => {
  const row = job(); row.event_type = "consultation_closed"; const f = fake(row);
  let time = now;
  const deliver = f.ports.deliver;
  f.ports.deliver = async (...args) => { const result = await deliver(...args); time += 90000; return result; };
  await runBatch(f.ports, () => time);
  assert(f.deliveries.length === 1 && f.failed.length === 1 && f.sent.length === 0);
});
Deno.test("SDK errors are not persisted as queue errors", async () => {
  const f = fake(job()); f.ports.deliver = async () => { throw Error("SECRET TOKEN BODY"); };
  await runBatch(f.ports, () => now); assert(!f.failed.join().includes("SECRET"));
});
Deno.test("FCM adapter sends data-only high-priority messages with bounded TTL", async () => {
  const previous = globalThis.fetch;
  let payload: any;
  globalThis.fetch = async (_url, init) => {
    payload = JSON.parse(String(init?.body));
    return Response.json({ name: "message" });
  };
  try {
    const result = await sendFcm({ client_email: "unused", private_key: "unused", project_id: "test" },
      "access", "device", { type: "consultation_invite" }, 40);
    assert(result.status === "sent" && payload.message.android.priority === "high" &&
      payload.message.android.ttl === "40s" && !payload.message.notification);
  } finally { globalThis.fetch = previous; }
});
Deno.test("FCM auth and invalid-argument failures do not delete valid tokens", async () => {
  const previous = globalThis.fetch;
  try {
    for (const status of [400, 401, 403, 500]) {
      globalThis.fetch = async () => Response.json({ error: { status: "FAILED" } }, { status });
      const result = await sendFcm({ client_email: "unused", private_key: "unused", project_id: "test" }, "access", "device", {}, 10);
      assert(result.status === "failed");
    }
    globalThis.fetch = async () => Response.json({ error: { details: [{
      "@type": "type.googleapis.com/google.firebase.fcm.v1.FcmError", errorCode: "UNREGISTERED",
    }] } }, { status: 404 });
    const result = await sendFcm({ client_email: "unused", private_key: "unused", project_id: "test" }, "access", "device", {}, 10);
    assert(result.status === "unregistered");
  } finally { globalThis.fetch = previous; }
});
