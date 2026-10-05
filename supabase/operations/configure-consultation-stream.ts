// Operator-only: use server environment variables, never Android or a public endpoint.
import { StreamClient } from "npm:@stream-io/node-sdk@0.8.8";
import { assertRestrictedGrants, CALL_TYPE, MEMBER_GRANTS } from "../functions/consultation-stream-token/handler.ts";

const apiKey = Deno.env.get("STREAM_API_KEY");
const secret = Deno.env.get("STREAM_API_SECRET");
if (!apiKey || !secret) throw new Error("Set STREAM_API_KEY and STREAM_API_SECRET in the operator environment.");
const stream = new StreamClient(apiKey, secret);
const existing = (await stream.video.listCallTypes()).call_types[CALL_TYPE];
const grants = { user: [], guest: [], anonymous: [], call_member: MEMBER_GRANTS };
if (existing) {
  await stream.video.updateCallType({ name: CALL_TYPE, grants });
} else {
  await stream.video.createCallType({ name: CALL_TYPE, grants });
}
assertRestrictedGrants((await stream.video.listCallTypes()).call_types[CALL_TYPE].grants);
console.log("Restricted consultation call type verified. Scheduled default type unchanged.");
