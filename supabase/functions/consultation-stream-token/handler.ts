export const CALL_TYPE = "consultation";
export const MEMBER_GRANTS = ["read-call", "join-call", "send-audio", "send-video"];

export class AccessError extends Error {
  constructor(public status: number, message: string) { super(message); }
}

export interface Access {
  requestId: string;
  patientId: string;
  doctorId: string;
  patientUserId: string;
  doctorUserId: string;
  callId: string;
  requestStatus: string;
  sessionStatus: string;
}

export function authorize(access: Access | null, userId: string): Access {
  // Return the same denial for missing and unrelated requests.
  if (!access || ![access.patientUserId, access.doctorUserId].includes(userId)) {
    throw new AccessError(403, "You cannot join this consultation.");
  }
  if (access.requestStatus !== "claimed" ||
      !["pending", "ready"].includes(access.sessionStatus)) {
    throw new AccessError(409, "This consultation is not available for video.");
  }
  if (!access.patientUserId || !access.doctorUserId ||
      access.patientUserId === access.doctorUserId ||
      access.callId !== `consultation-${access.requestId}`) {
    throw new AccessError(409, "The consultation session is not configured correctly.");
  }
  return access;
}

export function assertRestrictedGrants(grants: Record<string, string[]>): void {
  // Fail closed if the dedicated type drifts. Scheduled calls keep their own type.
  for (const role of ["user", "guest", "anonymous"]) {
    if ((grants[role] ?? []).length !== 0) {
      throw new AccessError(503, "Secure video permissions need configuration.");
    }
  }
  const member = grants.call_member ?? [];
  if (member.length !== MEMBER_GRANTS.length ||
      MEMBER_GRANTS.some((grant) => !member.includes(grant))) {
    throw new AccessError(503, "Secure video permissions need configuration.");
  }
}

export interface Dependencies {
  authenticate(token: string): Promise<string | null>;
  load(requestId: string): Promise<Access | null>;
  provision(access: Access): Promise<void>;
  markReady(access: Access): Promise<void>;
  token(userId: string, callCid: string): string;
  apiKey: string;
}

const headers = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Content-Type": "application/json",
  "Cache-Control": "no-store",
};
function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), { status, headers });
}

export function handler(deps: Dependencies) {
  return async (req: Request): Promise<Response> => {
    if (req.method === "OPTIONS") return new Response(null, { status: 204, headers });
    if (req.method !== "POST") return json({ error: "Method not allowed." }, 405);
    try {
      const bearer = /^Bearer\s+(\S+)$/i.exec(req.headers.get("Authorization") ?? "");
      if (!bearer) throw new AccessError(401, "Please sign in again.");
      const userId = await deps.authenticate(bearer[1]);
      if (!userId) throw new AccessError(401, "Please sign in again.");
      let body: { request_id?: unknown };
      try { body = await req.json(); } catch {
        throw new AccessError(400, "Invalid request.");
      }
      const id = body?.request_id;
      if (typeof id !== "string" ||
          !/^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(id)) {
        throw new AccessError(400, "A valid consultation request ID is required.");
      }
      const access = authorize(await deps.load(id.toLowerCase()), userId);
      await deps.provision(access);
      // The RPC locks and rechecks the request and session after the Stream operation.
      await deps.markReady(access);
      const current = authorize(await deps.load(access.requestId), userId);
      if (current.sessionStatus !== "ready" || current.callId !== access.callId ||
          current.patientUserId !== access.patientUserId || current.doctorUserId !== access.doctorUserId) {
        throw new AccessError(409, "The consultation changed. Please retry.");
      }
      return json({
        token: deps.token(userId, `${CALL_TYPE}:${access.callId}`),
        userId, callId: access.callId, callType: CALL_TYPE,
        requestId: access.requestId, apiKey: deps.apiKey,
      });
    } catch (error) {
      if (error instanceof AccessError) return json({ error: error.message }, error.status);
      // Do not expose provider responses, credentials, or patient information.
      console.error("Consultation video provisioning failed.");
      return json({ error: "Video could not be prepared. Please retry." }, 502);
    }
  };
}
