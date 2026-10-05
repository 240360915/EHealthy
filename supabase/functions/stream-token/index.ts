// supabase/functions/stream-token/index.ts
//
// Two modes:
//
// 1. Existing scheduled appointment mode:
//    Request has no consultationRequestId.
//    Returns the same normal Stream user token as before.
//
// 2. Secure on-demand consultation mode:
//    Request includes consultationRequestId.
//    Verifies that the caller is either:
//      - the patient for the request, OR
//      - the doctor who won the request.
//    Then creates the consultation Stream room server-side
//    and returns a token scoped to that consultation call only.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const SUPABASE_URL =
  Deno.env.get("SUPABASE_URL")!;

const SUPABASE_ANON_KEY =
  Deno.env.get("SUPABASE_ANON_KEY")!;

const SUPABASE_SERVICE_ROLE_KEY =
  Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

const STREAM_API_KEY =
  Deno.env.get("STREAM_API_KEY")!;

const STREAM_API_SECRET =
  Deno.env.get("STREAM_API_SECRET")!;


// -----------------------------------------------------------------------------
// Helpers
// -----------------------------------------------------------------------------

function base64UrlEncode(
  bytes: Uint8Array
): string {

  let binary = "";

  for (const b of bytes) {
    binary +=
      String.fromCharCode(b);
  }

  return btoa(binary)
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/, "");
}


async function signHS256(
  data: string,
  secret: string
): Promise<string> {

  const enc =
    new TextEncoder();

  const key =
    await crypto.subtle.importKey(
      "raw",
      enc.encode(secret),
      {
        name: "HMAC",
        hash: "SHA-256",
      },
      false,
      ["sign"]
    );

  const signature =
    await crypto.subtle.sign(
      "HMAC",
      key,
      enc.encode(data)
    );

  return base64UrlEncode(
    new Uint8Array(signature)
  );
}


async function createJwt(
  payload: Record<string, unknown>
): Promise<string> {

  const header = {
    alg: "HS256",
    typ: "JWT",
  };

  const enc =
    new TextEncoder();

  const headerB64 =
    base64UrlEncode(
      enc.encode(
        JSON.stringify(header)
      )
    );

  const payloadB64 =
    base64UrlEncode(
      enc.encode(
        JSON.stringify(payload)
      )
    );

  const signingInput =
    `${headerB64}.${payloadB64}`;

  const signature =
    await signHS256(
      signingInput,
      STREAM_API_SECRET
    );

  return `${signingInput}.${signature}`;
}


// -----------------------------------------------------------------------------
// Existing scheduled-call token
// -----------------------------------------------------------------------------

async function createStreamUserToken(
  userId: string
): Promise<string> {

  const now =
    Math.floor(
      Date.now() / 1000
    );

  return createJwt({
    user_id: userId,
    iat: now,
    exp: now + 60 * 60 * 4,
  });
}


// -----------------------------------------------------------------------------
// Consultation-only token
// -----------------------------------------------------------------------------

async function createConsultationToken(
  userId: string,
  callCid: string
): Promise<string> {

  const now =
    Math.floor(
      Date.now() / 1000
    );

  return createJwt({
    user_id: userId,

    /*
     * This token applies to this consultation
     * call only.
     */
    call_cids: [
      callCid
    ],

    iat: now,

    /*
     * 90 minutes is plenty for an on-demand
     * consultation without creating a long-lived
     * credential.
     */
    exp: now + 60 * 90,
  });
}


// -----------------------------------------------------------------------------
// Stream server token
// -----------------------------------------------------------------------------

async function createStreamServerToken():
  Promise<string> {

  /*
   * Server-side Stream requests use:
   *
   * {
   *   server: true
   * }
   */
  return createJwt({
    server: true,
  });
}


// -----------------------------------------------------------------------------
// Stream user creation
// -----------------------------------------------------------------------------

async function ensureStreamUsers(
  serverToken: string,
  patient: {
    user_id: string;
    name?: string | null;
    surname?: string | null;
  },
  doctor: {
    user_id: string;
    name?: string | null;
    surname?: string | null;
  }
) {

  const patientName =
    `${patient.name ?? ""} ${patient.surname ?? ""}`
      .trim() || "Patient";

  const doctorName =
    `Dr. ${doctor.name ?? ""} ${doctor.surname ?? ""}`
      .trim() || "Doctor";

  const response =
    await fetch(
      `https://video.stream-io-api.com/api/v2/users?api_key=${encodeURIComponent(
        STREAM_API_KEY
      )}`,
      {
        method: "POST",

        headers: {
          Authorization:
            serverToken,

          "stream-auth-type":
            "jwt",

          "Content-Type":
            "application/json",
        },

        body:
          JSON.stringify({
            users: {
              [patient.user_id]: {
                id:
                  patient.user_id,

                role:
                  "user",

                name:
                  patientName,
              },

              [doctor.user_id]: {
                id:
                  doctor.user_id,

                role:
                  "user",

                name:
                  doctorName,
              },
            },
          }),
      }
    );

  if (!response.ok) {

    const message =
      await response.text();

    console.error(
      "Stream user upsert failed:",
      message
    );

    throw new Error(
      "Could not prepare consultation users."
    );
  }
}


// -----------------------------------------------------------------------------
// Create/get consultation Stream call
// -----------------------------------------------------------------------------

async function ensureConsultationCall(
  serverToken: string,
  callId: string,
  requestId: string,
  patientUserId: string,
  doctorUserId: string
) {

  /*
   * IMPORTANT:
   *
   * We use a separate Stream call type:
   *
   * consultation
   *
   * Scheduled appointments can continue using:
   *
   * default
   */

  const callType =
    "consultation";

  const response =
    await fetch(
      `https://video.stream-io-api.com/api/v2/video/call/${callType}/${encodeURIComponent(
        callId
      )}?api_key=${encodeURIComponent(
        STREAM_API_KEY
      )}`,
      {
        method:
          "POST",

        headers: {
          Authorization:
            serverToken,

          "stream-auth-type":
            "jwt",

          "Content-Type":
            "application/json",
        },

        body:
          JSON.stringify({
            data: {

              created_by_id:
                patientUserId,

              members: [
                {
                  user_id:
                    patientUserId,

                  role:
                    "call_member",
                },

                {
                  user_id:
                    doctorUserId,

                  role:
                    "call_member",
                },
              ],

              custom: {
                consultation_request_id:
                  requestId,
              },
            },
          }),
      }
    );

  if (!response.ok) {

    const message =
      await response.text();

    console.error(
      "Stream consultation call creation failed:",
      message
    );

    throw new Error(
      "The consultation video room could not be created."
    );
  }
}


// -----------------------------------------------------------------------------
// Main function
// -----------------------------------------------------------------------------

Deno.serve(
  async (req) => {

    try {

      const authHeader =
        req.headers.get(
          "Authorization"
        );

      if (!authHeader) {

        return new Response(
          JSON.stringify({
            error:
              "Missing Authorization header",
          }),
          {
            status: 401,

            headers: {
              "Content-Type":
                "application/json",
            },
          }
        );
      }


      // -----------------------------------------------------------------------
      // Authenticate the Supabase user
      // -----------------------------------------------------------------------

      const supabase =
        createClient(
          SUPABASE_URL,
          SUPABASE_ANON_KEY,
          {
            global: {
              headers: {
                Authorization:
                  authHeader,
              },
            },
          }
        );

      const {
        data: userData,
        error: authError,
      } =
        await supabase.auth
          .getUser();


      if (
        authError ||
        !userData?.user
      ) {

        return new Response(
          JSON.stringify({
            error:
              "Invalid or expired session",
          }),
          {
            status: 401,

            headers: {
              "Content-Type":
                "application/json",
            },
          }
        );
      }


      const authUserId =
        userData.user.id;


      // -----------------------------------------------------------------------
      // Read optional request body
      // -----------------------------------------------------------------------

      let body:
        Record<string, unknown> = {};

      try {

        body =
          await req.json();

      } catch {

        /*
         * Existing scheduled calls may invoke this
         * function without a JSON body.
         *
         * That must continue working.
         */
      }


      const consultationRequestId =
        typeof body
          .consultationRequestId ===
          "string"
          ? body
            .consultationRequestId
          : null;


      // =======================================================================
      // MODE 1 — OLD SCHEDULED APPOINTMENT CALLS
      // =======================================================================

      if (
        !consultationRequestId
      ) {

        const token =
          await createStreamUserToken(
            authUserId
          );

        return new Response(
          JSON.stringify({
            token,
            userId:
              authUserId,
          }),
          {
            headers: {
              "Content-Type":
                "application/json",
            },
          }
        );
      }


      // =======================================================================
      // MODE 2 — ON-DEMAND CONSULTATION
      // =======================================================================

      const admin =
        createClient(
          SUPABASE_URL,
          SUPABASE_SERVICE_ROLE_KEY,
          {
            auth: {
              persistSession:
                false,

              autoRefreshToken:
                false,
            },
          }
        );


      // -----------------------------------------------------------------------
      // Load consultation
      // -----------------------------------------------------------------------

      const {
        data: consultation,
        error: consultationError,
      } =
        await admin
          .from(
            "consultation_requests"
          )
          .select(
            `
              id,
              patient_id,
              status,
              claimed_doctor_id
            `
          )
          .eq(
            "id",
            consultationRequestId
          )
          .maybeSingle();


      if (
        consultationError
      ) {

        console.error(
          "Consultation lookup error:",
          consultationError.message
        );

        return new Response(
          JSON.stringify({
            error:
              "Could not load consultation.",
          }),
          {
            status: 500,

            headers: {
              "Content-Type":
                "application/json",
            },
          }
        );
      }


      if (!consultation) {

        return new Response(
          JSON.stringify({
            error:
              "Consultation not found.",
          }),
          {
            status: 404,

            headers: {
              "Content-Type":
                "application/json",
            },
          }
        );
      }


      /*
       * No video room until the database has
       * confirmed a winning doctor.
       */
      if (
        consultation.status !==
          "claimed" ||
        !consultation
          .claimed_doctor_id
      ) {

        return new Response(
          JSON.stringify({
            error:
              "The consultation has not been claimed yet.",
          }),
          {
            status: 409,

            headers: {
              "Content-Type":
                "application/json",
            },
          }
        );
      }


      // -----------------------------------------------------------------------
      // Load patient
      // -----------------------------------------------------------------------

      const {
        data: patient,
        error: patientError,
      } =
        await admin
          .from(
            "patients"
          )
          .select(
            `
              id,
              user_id,
              name,
              surname
            `
          )
          .eq(
            "id",
            consultation.patient_id
          )
          .maybeSingle();


      if (
        patientError ||
        !patient?.user_id
      ) {

        console.error(
          "Patient identity lookup failed."
        );

        return new Response(
          JSON.stringify({
            error:
              "Patient identity could not be resolved.",
          }),
          {
            status: 500,

            headers: {
              "Content-Type":
                "application/json",
            },
          }
        );
      }


      // -----------------------------------------------------------------------
      // Load WINNING doctor
      // -----------------------------------------------------------------------

      const {
        data: doctor,
        error: doctorError,
      } =
        await admin
          .from(
            "doctors"
          )
          .select(
            `
              id,
              user_id,
              name,
              surname
            `
          )
          .eq(
            "id",
            consultation
              .claimed_doctor_id
          )
          .maybeSingle();


      if (
        doctorError ||
        !doctor?.user_id
      ) {

        console.error(
          "Winning doctor identity lookup failed."
        );

        return new Response(
          JSON.stringify({
            error:
              "Doctor identity could not be resolved.",
          }),
          {
            status: 500,

            headers: {
              "Content-Type":
                "application/json",
            },
          }
        );
      }


      // -----------------------------------------------------------------------
      // Authorization
      // -----------------------------------------------------------------------

      const isPatient =
        authUserId ===
        patient.user_id;

      const isWinningDoctor =
        authUserId ===
        doctor.user_id;


      if (
        !isPatient &&
        !isWinningDoctor
      ) {

        /*
         * This blocks every losing doctor from
         * receiving consultation video credentials.
         */

        return new Response(
          JSON.stringify({
            error:
              "You are not a participant in this consultation.",
          }),
          {
            status: 403,

            headers: {
              "Content-Type":
                "application/json",
            },
          }
        );
      }


      // -----------------------------------------------------------------------
      // Load consultation session
      // -----------------------------------------------------------------------

      const {
        data: session,
        error: sessionError,
      } =
        await admin
          .from(
            "consultation_sessions"
          )
          .select(
            `
              request_id,
              stream_call_id
            `
          )
          .eq(
            "request_id",
            consultationRequestId
          )
          .maybeSingle();


      if (
        sessionError
      ) {

        console.error(
          "Session lookup error:",
          sessionError.message
        );

        return new Response(
          JSON.stringify({
            error:
              "Could not load consultation session.",
          }),
          {
            status: 500,

            headers: {
              "Content-Type":
                "application/json",
            },
          }
        );
      }


      if (
        !session
          ?.stream_call_id
      ) {

        return new Response(
          JSON.stringify({
            error:
              "The consultation video room is still being prepared.",
          }),
          {
            status: 409,

            headers: {
              "Content-Type":
                "application/json",
            },
          }
        );
      }


      const callId =
        session.stream_call_id;

      const callType =
        "consultation";

      const callCid =
        `${callType}:${callId}`;


      // -----------------------------------------------------------------------
      // Prepare Stream server access
      // -----------------------------------------------------------------------

      const serverToken =
        await createStreamServerToken();


      // -----------------------------------------------------------------------
      // Ensure both Stream users exist
      // -----------------------------------------------------------------------

      await ensureStreamUsers(
        serverToken,
        patient,
        doctor
      );


      // -----------------------------------------------------------------------
      // Server creates the room.
      //
      // Android must use join(create = false)
      // for this consultation flow.
      // -----------------------------------------------------------------------

      await ensureConsultationCall(
        serverToken,
        callId,
        consultationRequestId,
        patient.user_id,
        doctor.user_id
      );


      // -----------------------------------------------------------------------
      // Token only for this consultation
      // -----------------------------------------------------------------------

      const token =
        await createConsultationToken(
          authUserId,
          callCid
        );


      return new Response(
        JSON.stringify({

          token,

          userId:
            authUserId,

          callId,

          callType,

          requestId:
            consultationRequestId,

        }),
        {
          status: 200,

          headers: {
            "Content-Type":
              "application/json",
          },
        }
      );


    } catch (error) {

      console.error(
        "stream-token unexpected error:",
        error
      );

      return new Response(
        JSON.stringify({
          error:
            "Could not prepare the video call.",
        }),
        {
          status: 500,

          headers: {
            "Content-Type":
              "application/json",
          },
        }
      );
    }
  }
);