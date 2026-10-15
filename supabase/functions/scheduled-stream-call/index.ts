// Separate from the working broadcast stream-token function.
// A scheduled appointment uses the SAME Stream "consultation" call type,
// server-owned room, JWT-scoped call access, and member roles as broadcast.
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SUPABASE_ANON_KEY = Deno.env.get("SUPABASE_ANON_KEY")!;
const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const STREAM_API_KEY = Deno.env.get("STREAM_API_KEY")!;
const STREAM_API_SECRET = Deno.env.get("STREAM_API_SECRET")!;

function response(data: Record<string, unknown>, status = 200): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "Content-Type": "application/json", "Cache-Control": "no-store" },
  });
}

function b64url(bytes: Uint8Array): string {
  let value = "";
  for (const b of bytes) value += String.fromCharCode(b);
  return btoa(value).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

async function jwt(payload: Record<string, unknown>): Promise<string> {
  const enc = new TextEncoder();
  const header = b64url(enc.encode(JSON.stringify({ alg: "HS256", typ: "JWT" })));
  const claims = b64url(enc.encode(JSON.stringify(payload)));
  const message = header + "." + claims;
  const key = await crypto.subtle.importKey("raw", enc.encode(STREAM_API_SECRET),
    { name: "HMAC", hash: "SHA-256" }, false, ["sign"]);
  const signature = await crypto.subtle.sign("HMAC", key, enc.encode(message));
  return message + "." + b64url(new Uint8Array(signature));
}

async function streamPost(path: string, serverToken: string, payload: unknown) {
  const result = await fetch(
    "https://video.stream-io-api.com/api/v2/" + path +
      "?api_key=" + encodeURIComponent(STREAM_API_KEY),
    {
      method: "POST",
      headers: {
        Authorization: serverToken,
        "stream-auth-type": "jwt",
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
    },
  );
  if (!result.ok) {
    console.error("Stream scheduled room provisioning HTTP", result.status,
      await result.text());
    throw new Error("Video room could not be prepared.");
  }
}

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return response({ error: "POST required" }, 405);
  try {
    const authorization = req.headers.get("Authorization") ?? "";
    if (!/^Bearer\s+\S+$/i.test(authorization)) {
      return response({ error: "Sign in to continue." }, 401);
    }

    // Authenticate each request ourselves, as the existing stream-token
    // function does. Never accept a user ID supplied by the app.
    const client = createClient(SUPABASE_URL, SUPABASE_ANON_KEY, {
      global: { headers: { Authorization: authorization } },
      auth: { persistSession: false, autoRefreshToken: false },
    });
    const { data: userResult, error: authError } = await client.auth.getUser();
    const authUserId = userResult?.user?.id;
    if (authError || !authUserId) {
      return response({ error: "Your session expired. Please sign in again." }, 401);
    }

    let input: { appointmentId?: unknown };
    try {
      input = await req.json();
    } catch {
      return response({ error: "An appointment ID is required." }, 400);
    }
    const appointmentId = input?.appointmentId;
    if (typeof appointmentId !== "string" ||
        !/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(appointmentId)) {
      return response({ error: "Invalid appointment ID." }, 400);
    }

    const admin = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY, {
      auth: { persistSession: false, autoRefreshToken: false },
    });
    const { data: appt, error: apptError } = await admin
      .from("appointments")
      .select("id,doctor_id,patient_id,status,appointment_type,price_minor,call_ended_at")
      .eq("id", appointmentId)
      .maybeSingle();
    if (apptError) throw apptError;
    if (!appt) return response({ error: "Appointment not found." }, 404);

    const [{ data: patient, error: pe }, { data: doctor, error: de }] =
      await Promise.all([
        admin.from("patients").select("id,user_id,name,surname")
          .eq("id", appt.patient_id).maybeSingle(),
        admin.from("doctors").select("id,user_id,name,surname,is_deactivated")
          .eq("id", appt.doctor_id).maybeSingle(),
      ]);
    if (pe || de || !patient?.user_id || !doctor?.user_id) {
      throw new Error("Could not resolve the authorized call participants.");
    }
    if (authUserId !== patient.user_id && authUserId !== doctor.user_id) {
      return response({ error: "You are not a participant in this appointment." }, 403);
    }
    if (doctor.is_deactivated) {
      return response({ error: "The doctor is not currently available." }, 403);
    }
    if (appt.appointment_type !== "online" || appt.status !== "confirmed" ||
        appt.call_ended_at !== null) {
      return response({ error: "This online consultation is not active." }, 409);
    }

    // Match the settled simulated payment to THIS appointment, doctor,
    // patient, currency and agreed price; no actual bank charges occur.
    const { data: payments, error: payError } = await admin
      .from("payments").select("amount")
      .eq("appointment_id", appointmentId)
      .eq("doctor_id", appt.doctor_id)
      .eq("patient_id", appt.patient_id)
      .eq("gateway_provider", "ehealthy_demo")
      .eq("payment_type", "scheduled_online")
      .eq("status", "paid")
      .eq("currency", "ZAR")
      .order("created_at", { ascending: false }).limit(1);
    if (payError) throw payError;
    const amount = Number(payments?.[0]?.amount);
    if (!Number.isFinite(amount) || !Number.isInteger(appt.price_minor) ||
        appt.price_minor <= 0 || Math.round(amount * 100) !== appt.price_minor) {
      return response({ error: "Complete DemoPay before joining the consultation." }, 402);
    }

    // Server-created room, just like a working on-demand/broadcast
    // consultation. Both phones get the same deterministic room identifier.
    const callType = "consultation";
    const callId = "scheduled-" + appt.id;
    const callCid = callType + ":" + callId;
    const serverToken = await jwt({ server: true });
    const pName = [patient.name, patient.surname].filter(Boolean).join(" ") || "Patient";
    const dName = "Dr. " + ([doctor.name, doctor.surname].filter(Boolean).join(" ") || "Doctor");

    await streamPost("users", serverToken, {
      users: {
        [patient.user_id]: { id: patient.user_id, role: "user", name: pName },
        [doctor.user_id]: { id: doctor.user_id, role: "user", name: dName },
      },
    });

    await streamPost(
      "video/call/" + callType + "/" + encodeURIComponent(callId),
      serverToken,
      {
        data: {
          created_by_id: patient.user_id,
          members: [
            { user_id: patient.user_id, role: "call_member" },
            { user_id: doctor.user_id, role: "call_member" },
          ],
          custom: { scheduled_appointment_id: appt.id },
        },
      },
    );

    const now = Math.floor(Date.now() / 1000);
    const token = await jwt({
      user_id: authUserId,
      call_cids: [callCid],
      iat: now,
      exp: now + 60 * 90,
    });
    return response({ token, userId: authUserId, appointmentId: appt.id, callType, callId });
  } catch (error) {
    console.error("Scheduled Stream provision failed", error);
    return response({ error: "Could not prepare secure scheduled video room." }, 500);
  }
});
