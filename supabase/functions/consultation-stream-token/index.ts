import { createClient } from "npm:@supabase/supabase-js@2.117.1";
import { StreamClient } from "npm:@stream-io/node-sdk@0.8.8";
import { AccessError, assertRestrictedGrants, CALL_TYPE, handler } from "./handler.ts";

const url = Deno.env.get("SUPABASE_URL");
const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
const apiKey = Deno.env.get("STREAM_API_KEY");
const secret = Deno.env.get("STREAM_API_SECRET");

if (!url || !serviceKey || !apiKey || !secret) {
  Deno.serve(() => new Response(JSON.stringify({ error: "Video service is not configured." }), {
    status: 503, headers: { "Content-Type": "application/json", "Cache-Control": "no-store" },
  }));
} else {
  const admin = createClient(url, serviceKey, {
    auth: { persistSession: false, autoRefreshToken: false },
    global: { fetch: (input, init) => fetch(input, { ...init, signal: AbortSignal.timeout(10_000) }) },
  });
  const stream = new StreamClient(apiKey, secret);
  Deno.serve(handler({
    apiKey,
    async authenticate(token) {
      const { data, error } = await admin.auth.getUser(token);
      return error ? null : data.user?.id ?? null;
    },
    async load(requestId) {
      const { data: request, error } = await admin.from("consultation_requests")
        .select("id,patient_id,claimed_doctor_id,status").eq("id", requestId).maybeSingle();
      if (error) throw error;
      if (!request?.claimed_doctor_id) return null;
      const [patient, doctor, session] = await Promise.all([
        admin.from("patients").select("user_id").eq("id", request.patient_id).maybeSingle(),
        admin.from("doctors").select("user_id").eq("id", request.claimed_doctor_id).maybeSingle(),
        admin.from("consultation_sessions").select("stream_call_id,status")
          .eq("request_id", requestId).maybeSingle(),
      ]);
      if (patient.error || doctor.error || session.error) throw new Error("Database lookup failed");
      return {
        requestId, patientId: request.patient_id, doctorId: request.claimed_doctor_id,
        patientUserId: patient.data?.user_id ?? "", doctorUserId: doctor.data?.user_id ?? "",
        callId: session.data?.stream_call_id ?? "", requestStatus: request.status,
        sessionStatus: session.data?.status ?? "missing",
      };
    },
    async provision(access) {
      const types = await stream.video.listCallTypes();
      const type = types.call_types[CALL_TYPE];
      if (!type) throw new AccessError(503, "Secure video permissions need configuration.");
      assertRestrictedGrants(type.grants);
      await stream.upsertUsers([
        { id: access.patientUserId, role: "user" },
        { id: access.doctorUserId, role: "user" },
      ]);
      const call = stream.video.call(CALL_TYPE, access.callId);
      const result = await call.getOrCreate({ data: {
        created_by_id: access.patientUserId,
        members: [
          { user_id: access.patientUserId, role: "call_member" },
          { user_id: access.doctorUserId, role: "call_member" },
        ],
        custom: { consultation_request_id: access.requestId },
      } });
      // getOrCreate does not repair existing calls. Never trust an unrelated or altered room.
      const members = result.members;
      if (result.call.ended_at || result.call.custom.consultation_request_id !== access.requestId ||
          members.length !== 2 || members.some((member) =>
            ![access.patientUserId, access.doctorUserId].includes(member.user_id) ||
            member.role !== "call_member")) {
        throw new AccessError(409, "The consultation room is unavailable.");
      }
    },
    async markReady(access) {
      const { error } = await admin.rpc("mark_consultation_video_ready", {
        p_request_id: access.requestId, p_call_id: access.callId,
        p_patient_id: access.patientId, p_doctor_id: access.doctorId,
      });
      if (error) throw new AccessError(409, "The consultation is no longer available. Please retry.");
    },
    token(userId, callCid) {
      return stream.generateCallToken({
        user_id: userId, call_cids: [callCid], validity_in_seconds: 300,
      });
    },
  }));
}
