export interface OutboxJob {
  outbox_id: string; request_id: string; doctor_id: string; doctor_user_id: string;
  event_type: string; request_version: number; attempts: number;
  payload: { expires_at?: string };
  device_tokens: Array<{ token: string; installation_id: string; platform: string }>;
}
export interface Delivery { status: "sent" | "unregistered" | "failed"; code?: string }
export interface WorkerPorts {
  claim(): Promise<OutboxJob[]>;
  sent(id: string): Promise<void>;
  failed(id: string, error: string): Promise<void>;
  deliver(token: string, data: Record<string, string>, ttl: number): Promise<Delivery>;
  removeToken(owner: string, installation: string, token: string): Promise<void>;
}

/** Payload allow-list: never forward patient information or arbitrary outbox JSON to phones. */
export function messageFor(job: OutboxJob, now: number) {
  if (!["consultation_invite", "consultation_closed"].includes(job.event_type)) throw Error("Unsupported event");
  if (!Number.isInteger(job.request_version) || job.request_version < 1) throw Error("Invalid version");
  const data: Record<string, string> = {
    type: job.event_type, request_id: job.request_id, version: String(job.request_version),
    recipient_user_id: job.doctor_user_id,
  };
  let ttl = 300;
  if (job.event_type === "consultation_invite") {
    const expiry = Date.parse(job.payload.expires_at ?? "");
    if (!Number.isFinite(expiry)) throw Error("Missing invitation expiry");
    if (expiry <= now) return null;
    ttl = Math.max(1, Math.min(60, Math.floor((expiry - now) / 1000)));
    data.expires_at = job.payload.expires_at!;
  }
  return { data, ttl };
}

/** At-least-once delivery: partial successes may be resent; phones deduplicate by request/version. */
export async function runBatch(ports: WorkerPorts, now = () => Date.now()) {
  const deadline = now() + 90000;
  const jobs = await ports.claim();
  const counts = { claimed: jobs.length, sent: 0, failed: 0, expired: 0 };
  for (const job of jobs) {
    try {
      if (now() >= deadline) throw Error("Worker time budget reached");
      const message = messageFor(job, now());
      if (!message) {
        await ports.sent(job.outbox_id); counts.expired++; continue;
      }
      const devices = job.device_tokens.filter(d => d.platform === "android");
      if (!devices.length) throw Error("No registered Android device");
      let successes = 0;
      let failures = 0;
      // Bounded batch (5 rows), sequential devices, individual network timeouts in the adapter.
      // Keep the function invocation under the database's five-minute reclaim lease.
      for (const device of devices) {
        if (now() >= deadline) throw Error("Worker time budget reached");
        const current = messageFor(job, now());
        if (!current) break;
        const result = await ports.deliver(device.token, current.data, current.ttl);
        if (result.status === "sent") successes++;
        else if (result.status === "unregistered")
          await ports.removeToken(job.doctor_user_id, device.installation_id, device.token);
        else failures++;
      }
      if (failures) throw Error("FCM delivery failed for one or more devices");
      if (!successes && messageFor(job, now()) !== null) throw Error("No reachable registered Android device");
      await ports.sent(job.outbox_id); counts.sent++;
    } catch (error) {
      // Do not persist SDK errors, response bodies, device tokens or credentials.
      const safe = error instanceof Error && [
        "Unsupported event", "Invalid version", "Missing invitation expiry",
        "No registered Android device", "FCM delivery failed for one or more devices",
        "No reachable registered Android device",
        "Worker time budget reached",
      ].includes(error.message) ? error.message : "Worker delivery or acknowledgement failed";
      await ports.failed(job.outbox_id, safe);
      counts.failed++;
    }
  }
  return counts;
}
