// Adapted from the project's existing appointment-cron FCM v1 implementation.
// Scheduled appointment code is deliberately unchanged.
import type { Delivery } from "../consultation-worker/worker.ts";
export interface ServiceAccount { client_email: string; private_key: string; project_id: string }
function base64url(bytes: Uint8Array) {
  let value = "";
  for (const byte of bytes) value += String.fromCharCode(byte);
  return btoa(value).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}
export function readServiceAccount(encoded: string): ServiceAccount {
  const value = JSON.parse(atob(encoded));
  if (!value.client_email || !value.private_key || !value.project_id) throw Error("Invalid FCM configuration");
  return value;
}
export async function accessToken(sa: ServiceAccount): Promise<string> {
  const encode = new TextEncoder();
  const now = Math.floor(Date.now() / 1000);
  const header = base64url(encode.encode(JSON.stringify({ alg: "RS256", typ: "JWT" })));
  const claims = base64url(encode.encode(JSON.stringify({
    iss: sa.client_email, scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token", iat: now, exp: now + 3600,
  })));
  const pem = sa.private_key.replace(/-----BEGIN PRIVATE KEY-----|-----END PRIVATE KEY-----|\s/g, "");
  const key = await crypto.subtle.importKey("pkcs8", Uint8Array.from(atob(pem), c => c.charCodeAt(0)),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["sign"]);
  const signature = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, encode.encode(header + "." + claims));
  const response = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST", signal: AbortSignal.timeout(8000),
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: header + "." + claims + "." + base64url(new Uint8Array(signature)) }),
  });
  const body = await response.json();
  if (!response.ok || typeof body.access_token !== "string") throw Error("FCM authentication failed");
  return body.access_token;
}
export async function sendFcm(sa: ServiceAccount, access: string, token: string,
  data: Record<string, string>, ttl: number): Promise<Delivery> {
  try {
    const response = await fetch("https://fcm.googleapis.com/v1/projects/" + sa.project_id + "/messages:send", {
      method: "POST", signal: AbortSignal.timeout(8000),
      headers: { Authorization: "Bearer " + access, "Content-Type": "application/json" },
      body: JSON.stringify({ message: { token, data, android: { priority: "high", ttl: ttl + "s" } } }),
    });
    if (response.ok) return { status: "sent" };
    const body = await response.json().catch(() => ({}));
    const unregistered = response.status === 404 && body.error?.details?.some(
      (d: { errorCode?: string; "@type"?: string }) => d.errorCode === "UNREGISTERED" &&
        d["@type"] === "type.googleapis.com/google.firebase.fcm.v1.FcmError");
    return unregistered ? { status: "unregistered" } : { status: "failed", code: String(response.status) };
  } catch { return { status: "failed", code: "network" }; }
}
