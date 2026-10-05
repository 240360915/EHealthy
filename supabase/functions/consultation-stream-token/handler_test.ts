import { AccessError, assertRestrictedGrants, handler, MEMBER_GRANTS } from "./handler.ts";
import type { Access, Dependencies } from "./handler.ts";

function equal(actual: unknown, expected: unknown) {
  if (JSON.stringify(actual) !== JSON.stringify(expected)) throw new Error(`${JSON.stringify(actual)} != ${JSON.stringify(expected)}`);
}
const id = "a1111111-1111-4111-8111-111111111111";
const initial: Access = {
  requestId: id, patientId: "patient-profile", doctorId: "doctor-profile",
  patientUserId: "patient-auth", doctorUserId: "winner-auth", callId: `consultation-${id}`,
  requestStatus: "claimed", sessionStatus: "pending",
};
function fixture() {
  let user: string | null = "patient-auth";
  let row: Access | null = { ...initial };
  const calls: string[] = [];
  const deps: Dependencies = {
    apiKey: "public-api-key",
    authenticate: () => Promise.resolve(user),
    load: () => Promise.resolve(row ? { ...row } : null),
    provision: () => { calls.push("provision"); return Promise.resolve(); },
    markReady: () => {
      calls.push("ready");
      if (row) row.sessionStatus = "ready";
      return Promise.resolve();
    },
    token: (userId, cid) => { calls.push(`token:${userId}:${cid}`); return "secret-token"; },
  };
  return { deps, calls, setUser: (value: string | null) => user = value,
    setRow: (value: Access | null) => row = value };
}
function request(body: unknown = { request_id: id }, auth = "Bearer jwt") {
  return new Request("https://example.test", {
    method: "POST", headers: { Authorization: auth }, body: JSON.stringify(body),
  });
}

for (const user of ["patient-auth", "winner-auth"]) {
  Deno.test(`${user} receives only the authoritative consultation room`, async () => {
    const f = fixture(); f.setUser(user);
    const response = await handler(f.deps)(request({ request_id: id, callId: "attacker-room", userId: "attacker" }));
    equal(response.status, 200);
    equal(response.headers.get("Cache-Control"), "no-store");
    equal((await response.json()).callId, initial.callId);
    equal(f.calls, ["provision", "ready", `token:${user}:consultation:${initial.callId}`]);
  });
}
for (const user of ["losing-doctor-auth", "unrelated-patient", "patient-profile", "doctor-profile", null]) {
  Deno.test(`${user} receives no video access`, async () => {
    const f = fixture(); f.setUser(user);
    equal((await handler(f.deps)(request())).status, user ? 403 : 401);
    equal(f.calls, []);
  });
}
for (const state of ["pending", "cancelled", "expired", "completed", "failed"]) {
  Deno.test(`request state ${state} cannot provision`, async () => {
    const f = fixture(); f.setRow({ ...initial, requestStatus: state });
    equal((await handler(f.deps)(request())).status, 409); equal(f.calls, []);
  });
}
for (const state of ["missing", "closed", "failed", "unexpected"]) {
  Deno.test(`session state ${state} cannot provision`, async () => {
    const f = fixture(); f.setRow({ ...initial, sessionStatus: state });
    equal((await handler(f.deps)(request())).status, 409); equal(f.calls, []);
  });
}
Deno.test("missing request is denied without provisioning", async () => {
  const f = fixture(); f.setRow(null);
  equal((await handler(f.deps)(request())).status, 403); equal(f.calls, []);
});
Deno.test("malformed request and authorization fail closed", async () => {
  const f = fixture();
  for (const body of [null, {}, { request_id: "appointment-123" }]) {
    equal((await handler(f.deps)(request(body))).status, 400);
  }
  equal((await handler(f.deps)(request({}, ""))).status, 401);
  equal(f.calls, []);
});
Deno.test("room substitution is rejected", async () => {
  const f = fixture(); f.setRow({ ...initial, callId: "appointment-id" });
  equal((await handler(f.deps)(request())).status, 409); equal(f.calls, []);
});
Deno.test("Stream failure cannot mark ready or issue credentials; retry can recover", async () => {
  const f = fixture(); const provision = f.deps.provision;
  f.deps.provision = () => Promise.reject(new Error("provider-secret"));
  const response = await handler(f.deps)(request());
  equal(response.status, 502); equal(f.calls, []);
  equal((await response.text()).includes("provider-secret"), false);
  f.deps.provision = provision;
  equal((await handler(f.deps)(request())).status, 200);
});
Deno.test("closure during provisioning denies issuance", async () => {
  const f = fixture();
  f.deps.markReady = () => Promise.reject(new AccessError(409, "Session closed"));
  equal((await handler(f.deps)(request())).status, 409);
  equal(f.calls, ["provision"]);
});
Deno.test("winner changes before final check denies issuance", async () => {
  const f = fixture();
  f.deps.markReady = () => {
    f.setRow({ ...initial, sessionStatus: "ready", doctorUserId: "different-doctor" });
    return Promise.resolve();
  };
  equal((await handler(f.deps)(request())).status, 409);
  equal(f.calls, ["provision"]);
});
Deno.test("retries and simultaneous requests reuse the same database room", async () => {
  const f = fixture();
  const responses = await Promise.all([handler(f.deps)(request()), handler(f.deps)(request())]);
  for (const response of responses) {
    equal(response.status, 200); equal((await response.json()).callId, initial.callId);
  }
});
Deno.test("restricted type rejects legacy user joins and member privilege escalation", () => {
  const good = { user: [], guest: [], anonymous: [], call_member: MEMBER_GRANTS };
  assertRestrictedGrants(good);
  for (const bad of [
    { ...good, user: ["join-call"] },
    { ...good, guest: ["create-call"] },
    { ...good, anonymous: ["read-call"] },
    { ...good, call_member: [...MEMBER_GRANTS, "update-call-member"] },
    { ...good, call_member: ["join-call"] },
  ]) {
    let denied = false;
    try { assertRestrictedGrants(bad); } catch { denied = true; }
    equal(denied, true);
  }
});
Deno.test("preflight and unsupported methods do not access provider", async () => {
  const f = fixture();
  equal((await handler(f.deps)(new Request("https://example.test", { method: "OPTIONS" }))).status, 204);
  equal((await handler(f.deps)(new Request("https://example.test"))).status, 405);
  equal(f.calls, []);
});
