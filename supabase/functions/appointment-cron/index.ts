// supabase/functions/appointment-cron/index.ts
//
// Runs every minute via pg_cron. In one pass it:
//   1. Sends the 5-minute-before reminder (email + push) to patient & doctor
//   2. Cancels appointments where the doctor never joined within 5 min of the
//      scheduled time (online only) — full refund
//   3. Cancels appointments where the doctor joined but the patient never
//      showed within 15 min of the scheduled time (online only) — 90% refund
//   4. Marks a call "started" once both parties have joined, and auto-completes
//      + releases funds 1 hour after that
//
// In-person completion (doctor marks complete / completion-code verification)
// is handled by direct app calls, not this cron job.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const RESEND_API_KEY = Deno.env.get("RESEND_API_KEY")!;
const FIREBASE_SERVICE_ACCOUNT_B64 = Deno.env.get("FIREBASE_SERVICE_ACCOUNT_B64")!;

const supabase = createClient(SUPABASE_URL, SERVICE_ROLE_KEY);

// ---------------------------------------------------------------------------
// FCM v1 push — needs an OAuth access token signed with the service account.
// ---------------------------------------------------------------------------

interface ServiceAccount {
  client_email: string;
  private_key: string;
  project_id: string;
}

function base64UrlEncode(bytes: Uint8Array): string {
  let binary = "";
  for (const b of bytes) binary += String.fromCharCode(b);
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function pemToArrayBuffer(pem: string): ArrayBuffer {
  const clean = pem
    .replace(/-----BEGIN PRIVATE KEY-----/, "")
    .replace(/-----END PRIVATE KEY-----/, "")
    .replace(/\s/g, "");
  const binary = atob(clean);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
  return bytes.buffer;
}

async function getFcmAccessToken(sa: ServiceAccount): Promise<string> {
  const header = { alg: "RS256", typ: "JWT" };
  const now = Math.floor(Date.now() / 1000);
  const claims = {
    iss: sa.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
  };

  const enc = new TextEncoder();
  const headerB64 = base64UrlEncode(enc.encode(JSON.stringify(header)));
  const claimsB64 = base64UrlEncode(enc.encode(JSON.stringify(claims)));
  const signingInput = `${headerB64}.${claimsB64}`;

  const keyData = pemToArrayBuffer(sa.private_key);
  const cryptoKey = await crypto.subtle.importKey(
    "pkcs8",
    keyData,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"]
  );
  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    cryptoKey,
    enc.encode(signingInput)
  );
  const jwt = `${signingInput}.${base64UrlEncode(new Uint8Array(signature))}`;

  const tokenRes = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: jwt,
    }),
  });
  const tokenJson = await tokenRes.json();
  if (!tokenJson.access_token) {
    console.error("Failed to get FCM access token:", tokenJson);
    throw new Error("FCM auth failed");
  }
  return tokenJson.access_token;
}
async function sendDataMessage(
  sa: ServiceAccount,
  accessToken: string,
  token: string,
  data: Record<string, string>
) {
  try {
    const res = await fetch(
      `https://fcm.googleapis.com/v1/projects/${sa.project_id}/messages:send`,
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${accessToken}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          message: {
            token,
            data,
            android: { priority: "high" },
          },
        }),
      }
    );
    if (!res.ok) {
      console.error("FCM data send failed:", await res.text());
    }
  } catch (e) {
    console.error("FCM data send error:", e);
  }
}

async function sendPush(
  sa: ServiceAccount,
  accessToken: string,
  token: string,
  title: string,
  body: string
) {
  try {
    const res = await fetch(
      `https://fcm.googleapis.com/v1/projects/${sa.project_id}/messages:send`,
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${accessToken}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          message: {
            token,
            notification: { title, body },
          },
        }),
      }
    );
    if (!res.ok) {
      console.error("FCM send failed:", await res.text());
    }
  } catch (e) {
    console.error("FCM send error:", e);
  }
}

// ---------------------------------------------------------------------------
// Resend email
// ---------------------------------------------------------------------------

async function sendEmail(to: string, subject: string, html: string) {
  try {
    const res = await fetch("https://api.resend.com/emails", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${RESEND_API_KEY}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        from: "eHealthy Connect <onboarding@resend.dev>",
        to: [to],
        subject,
        html,
      }),
    });
    if (!res.ok) {
      console.error("Resend send failed:", await res.text());
    }
  } catch (e) {
    console.error("Resend send error:", e);
  }
}

// ---------------------------------------------------------------------------
// Shared types
// ---------------------------------------------------------------------------

interface AppointmentRow {
  id: string;
  patient_id: string;
  doctor_id: string;
  date: string;
  time: string;
  reason: string | null;
  status: string | null;
  appointment_type: string | null;
  reminder_sent: boolean;
  ring_sent: boolean;
  call_joined_doctor_at: string | null;
  call_joined_patient_at: string | null;
  call_started_at: string | null;
  amount_paid: number | null;
}

interface PersonRow {
  id: string;
  name: string | null;
  surname: string | null;
  email: string | null;
  fcm_token: string | null;
  email_notifications: boolean | null;
}

function scheduledDateTime(a: AppointmentRow): Date {
  return new Date(`${a.date}T${a.time.slice(0, 5)}:00+02:00`);
}

// ---------------------------------------------------------------------------
// Main handler
// ---------------------------------------------------------------------------

Deno.serve(async (_req) => {
  const now = new Date();

  let sa: ServiceAccount | null = null;
  let fcmAccessToken: string | null = null;
  try {
    const decoded = atob(FIREBASE_SERVICE_ACCOUNT_B64);
    sa = JSON.parse(decoded);
    fcmAccessToken = await getFcmAccessToken(sa!);
  } catch (e) {
    console.error("Could not init FCM auth — push notifications will be skipped this run:", e);
  }

  // Pull every appointment that's still "live" (not cancelled/completed) and
  // within a reasonable window around now, so we don't scan the whole table.
  const { data: appointments, error } = await supabase
    .from("appointments")
    .select(
      "id, patient_id, doctor_id, date, time, reason, status, appointment_type, reminder_sent, ring_sent, call_joined_doctor_at, call_joined_patient_at, call_started_at, amount_paid"
    )
    .not("status", "in", '("cancelled","completed")');

  if (error) {
    console.error("Failed to fetch appointments:", error);
    return new Response(JSON.stringify({ error: error.message }), { status: 500 });
  }

  const rows = (appointments ?? []) as AppointmentRow[];

  for (const appt of rows) {
    const scheduledAt = scheduledDateTime(appt);
    const minutesUntil = (scheduledAt.getTime() - now.getTime()) / 60000;

    // ---- 1. 5-minute reminder ----
    if (!appt.reminder_sent && minutesUntil <= 5 && minutesUntil >= 0) {
      await handleReminder(appt);
    }
    // ---- 0. Auto-dial: appointment time has arrived, ring both parties ----
    if (
      appt.appointment_type === "online" &&
      appt.status === "confirmed" &&
      !appt.ring_sent &&
      minutesUntil <= 0 &&
      minutesUntil >= -1
    ) {
      await ringBothParties(appt);
    }

    if (appt.appointment_type === "online") {
      // ---- 2. Doctor no-show: 5+ min past scheduled time, doctor never joined ----
      if (
        minutesUntil <= -5 &&
        !appt.call_joined_doctor_at &&
        !appt.call_started_at
      ) {
        await cancelAppointment(appt, "doctor_no_show", 100);
        continue;
      }

      // ---- 3. Patient no-show: doctor joined, patient didn't, 15+ min past ----
      if (
        minutesUntil <= -15 &&
        appt.call_joined_doctor_at &&
        !appt.call_joined_patient_at &&
        !appt.call_started_at
      ) {
        await cancelAppointment(appt, "patient_no_show", 90);
        continue;
      }

      // ---- Mark call as started once both have joined ----
      if (
        appt.call_joined_doctor_at &&
        appt.call_joined_patient_at &&
        !appt.call_started_at
      ) {
        await supabase
          .from("appointments")
          .update({ call_started_at: now.toISOString(), status: "confirmed" })
          .eq("id", appt.id);
        continue;
      }

      // ---- 4. Auto-complete 1 hour after call started ----
      if (appt.call_started_at) {
        const startedAt = new Date(appt.call_started_at);
        const minutesSinceStart = (now.getTime() - startedAt.getTime()) / 60000;
        if (minutesSinceStart >= 60) {
          await supabase
            .from("appointments")
            .update({ status: "completed", funds_released: true })
            .eq("id", appt.id);
        }
      }
    }
  }
  async function ringBothParties(appt: AppointmentRow) {
    const { data: patient } = await supabase
      .from("patients")
      .select("id, name, surname, email, fcm_token, email_notifications")
      .eq("id", appt.patient_id)
      .maybeSingle<PersonRow>();

    const { data: doctor } = await supabase
      .from("doctors")
      .select("id, name, surname, email, fcm_token, email_notifications")
      .eq("id", appt.doctor_id)
      .maybeSingle<PersonRow>();

    const doctorName = doctor ? `Dr. ${doctor.name ?? ""} ${doctor.surname ?? ""}`.trim() : "your doctor";
    const patientName = patient ? `${patient.name ?? ""} ${patient.surname ?? ""}`.trim() : "your patient";

    if (sa && fcmAccessToken) {
      if (patient?.fcm_token) {
        await sendDataMessage(sa, fcmAccessToken, patient.fcm_token, {
          type: "incoming_call",
          appointmentId: appt.id,
          role: "patient",
          otherPartyName: doctorName,
        });
      }
      if (doctor?.fcm_token) {
        await sendDataMessage(sa, fcmAccessToken, doctor.fcm_token, {
          type: "incoming_call",
          appointmentId: appt.id,
          role: "doctor",
          otherPartyName: patientName,
        });
      }
    }

    await supabase.from("appointments").update({ ring_sent: true }).eq("id", appt.id);
  }

  async function handleReminder(appt: AppointmentRow) {
    const { data: patient } = await supabase
      .from("patients")
      .select("id, name, surname, email, fcm_token, email_notifications")
      .eq("id", appt.patient_id)
      .maybeSingle<PersonRow>();

    const { data: doctor } = await supabase
      .from("doctors")
      .select("id, name, surname, email, fcm_token, email_notifications")
      .eq("id", appt.doctor_id)
      .maybeSingle<PersonRow>();

    const timeLabel = appt.time.slice(0, 5);
    const doctorName = doctor ? `Dr. ${doctor.name ?? ""} ${doctor.surname ?? ""}`.trim() : "your doctor";
    const patientName = patient ? `${patient.name ?? ""} ${patient.surname ?? ""}`.trim() : "your patient";

    if (patient?.email && patient.email_notifications !== false) {
      await sendEmail(
        patient.email,
        "Your appointment starts in 5 minutes",
        `<p>Hi ${patient.name ?? ""},</p><p>Your appointment with ${doctorName} at ${timeLabel} is starting soon. Please be ready.</p>`
      );
    }
    if (doctor?.email && doctor.email_notifications !== false) {
      await sendEmail(
        doctor.email,
        "Your appointment starts in 5 minutes",
        `<p>Hi Dr. ${doctor.name ?? ""},</p><p>Your appointment with ${patientName} at ${timeLabel} is starting soon.</p>`
      );
    }

    if (sa && fcmAccessToken) {
      if (patient?.fcm_token && patient.email_notifications !== false) {
        await sendPush(
          sa,
          fcmAccessToken,
          patient.fcm_token,
          "Appointment in 5 minutes",
          `Your appointment with ${doctorName} is starting soon. Please be ready.`
        );
      }
      if (doctor?.fcm_token && doctor.email_notifications !== false) {
        await sendPush(
          sa,
          fcmAccessToken,
          doctor.fcm_token,
          "Appointment in 5 minutes",
          `Your appointment with ${patientName} is starting soon.`
        );
      }
    }

    await supabase.from("appointments").update({ reminder_sent: true }).eq("id", appt.id);
  }

  async function cancelAppointment(
    appt: AppointmentRow,
    reason: "doctor_no_show" | "patient_no_show",
    refundPercent: number
  ) {
    await supabase
      .from("appointments")
      .update({
        status: "cancelled",
        cancelled_reason: reason,
        refund_percent: refundPercent,
      })
      .eq("id", appt.id);

    const { data: patient } = await supabase
      .from("patients")
      .select("id, name, surname, email, fcm_token, email_notifications")
      .eq("id", appt.patient_id)
      .maybeSingle<PersonRow>();

    if (patient?.email) {
      const msg =
        reason === "doctor_no_show"
          ? "Unfortunately the doctor didn't join your appointment. You've been fully refunded, and you're welcome to rebook."
          : `We noticed you weren't able to join in time, so this appointment has been cancelled. A ${refundPercent}% refund has been processed.`;
      await sendEmail(patient.email, "Your appointment was cancelled", `<p>${msg}</p>`);
    }
    if (sa && fcmAccessToken && patient?.fcm_token) {
      await sendPush(
        sa,
        fcmAccessToken,
        patient.fcm_token,
        "Appointment cancelled",
        reason === "doctor_no_show"
          ? "The doctor didn't join — you've been fully refunded."
          : `Cancelled — a ${refundPercent}% refund has been processed.`
      );
    }
  }

  return new Response(JSON.stringify({ processed: rows.length }), {
    headers: { "Content-Type": "application/json" },
  });
});