import { createClient } from "https://esm.sh/@supabase/supabase-js@2.117.2";
import { accessToken, readServiceAccount, sendFcm } from "../_shared/fcm.ts";
import { runBatch } from "./worker.ts";
import type { OutboxJob } from "./worker.ts";

Deno.serve(async request => {
  if (request.method !== "POST") return new Response("Method not allowed", { status: 405 });
  const secret = Deno.env.get("CONSULTATION_WORKER_SECRET");
  // A dedicated scheduler secret, not an end-user JWT. Missing configuration fails closed.
  if (!secret || request.headers.get("x-consultation-worker-secret") !== secret)
    return new Response("Unauthorized", { status: 401 });
  try {
    const url = Deno.env.get("SUPABASE_URL")!;
    const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
    const client = createClient(url, serviceKey, {
      auth: { persistSession: false, autoRefreshToken: false },
      global: { fetch: (input, init) => fetch(input, { ...init, signal: AbortSignal.timeout(8000) }) },
    });
    const sa = readServiceAccount(Deno.env.get("FIREBASE_SERVICE_ACCOUNT_B64")!);
    const access = await accessToken(sa);
    const rpc = async (name: string, args: Record<string, unknown> = {}) => {
      const { data, error } = await client.rpc(name, args);
      if (error) throw Error("Database operation failed");
      return data;
    };
    await rpc("expire_consultation_requests");
    const result = await runBatch({
      claim: async () => await rpc("claim_notification_outbox_batch", { p_limit: 5 }) as OutboxJob[],
      sent: async id => {
        if (await rpc("mark_notification_outbox_sent", { p_outbox_id: id }) !== true)
          throw Error("Acknowledgement rejected");
      },
      failed: async (id, error) => {
        if (await rpc("mark_notification_outbox_failed", { p_outbox_id: id, p_error: error }) !== true)
          throw Error("Failure acknowledgement rejected");
      },
      deliver: (token, data, ttl) => sendFcm(sa, access, token, data, ttl),
      removeToken: async (owner, installation, token) => {
        const { error } = await client.from("device_tokens").delete()
          .eq("owner_user_id", owner).eq("installation_id", installation).eq("token", token);
        if (error) throw Error("Token cleanup failed");
      },
    });
    return Response.json(result);
  } catch {
    return Response.json({ error: "Consultation worker failed; inspect configuration and queue status." }, { status: 500 });
  }
});
