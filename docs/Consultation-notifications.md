# Consultation notifications: implementation and activation

## Implemented

Android uses the deployed RPC signatures for device registration/unregistration, explicit doctor availability, heartbeat, request creation, claim, decline and patient cancellation. Profile ownership resolves through `doctors.user_id` / `patients.user_id`.

The patient dashboard opens live consultation requests. A doctor's profile offers a private request. Prices are read from the server. No broadcast tariff is inserted, configured or sent by Android. The deployed server currently rejects broadcast creation because its tariff is unset.

Doctor dashboard → Availability registers the current installation, enables general-consultation availability and starts a visible foreground service. It refreshes the heartbeat every 45 seconds. Loss of connectivity stops the service; the server excludes stale heartbeats after two minutes. Android may stop the service, including its data-sync time limit; the doctor must enable availability again. This is not a guarantee of permanent background availability.

FCM invitation data contains only request ID, recipient Auth ID, version and expiry. WorkManager checks the authenticated recipient and server state before displaying a private, per-request incoming-call notification. The incoming screen accepts/declines through RPCs. No client-side winner arbitration exists.

`claim_consultation_request` locks the request in PostgreSQL, rechecks approval/presence/service scope and takes the doctor's occupancy lock. Only its successful winner is shown as accepted. The database queues close events for losing doctors. Android persists terminal versions, so delayed invites cannot reopen a closed request. Active invitations also check server state every two seconds, subject to network latency; this catches missed close pushes and the winning doctor's other devices. Notifications time out at expiry; the visible screen has an independent expiry timer.

Scheduled notifications (`incoming_call`, notification ID 9999), appointment routes, `appointment-cron`, `stream-token` and `Callscreen.kt` retain their existing behavior. Device registration also refreshes the legacy profile `fcm_token` used by scheduled calls. New consultation notifications use a separate channel and per-request ID/tag.

## Worker

`supabase/functions/consultation-worker/index.ts` uses the existing Firebase service-account format (`FIREBASE_SERVICE_ACCOUNT_B64`) and FCM v1 signing/sending approach. The shared adapter is in `_shared/fcm.ts`; the existing appointment worker was not rewritten.

Each run authenticates a dedicated scheduler secret, expires overdue requests, claims five outbox rows, and sends data-only high-priority Android messages to every registered Android device for the recipient. Expired invitations are discarded. Partial delivery fails the row for retry; successful devices may receive duplicates, which Android suppresses. Only definitive FCM `UNREGISTERED` tokens are removed, matched on owner, installation and token. Errors stored in the outbox contain no tokens, credentials or response bodies.

The worker limits work to approximately 90 seconds plus bounded acknowledgements, below the five-minute reclaim lease. Each network request has an eight-second timeout. The current acknowledgement RPCs have no lease-generation fencing; that remains a backend hardening task. Five rows per invocation is intended for this small project; measure queue age and scale throughput before a larger rollout. FCM acceptance is not proof a phone displayed an invitation.

## Activation steps (not a schema migration)

1. Set an independently generated strong `CONSULTATION_WORKER_SECRET` in Supabase Edge Function secrets. Reuse the existing `FIREBASE_SERVICE_ACCOUNT_B64`; do not put either in Android or Git.
2. Store the same worker secret in Supabase Vault under `consultation_worker_secret`. Do this through a secure operator path; never paste a literal secret into committed SQL or logs.
3. Deploy only `consultation-worker` with gateway JWT verification disabled. The function performs its own dedicated-secret check and rejects requests without it. Do not redeploy the unchanged scheduled-call functions.

   `supabase functions deploy consultation-worker --no-verify-jwt`

4. After the secret and function exist, run `supabase/operations/schedule-consultation-worker.sql`. This adds a separate ten-second scheduler job using existing pg_cron/pg_net/Vault. A once-per-minute schedule is unsuitable for 60-second invitations. Review workload and plan limits before enabling.
5. Install the updated APK on designated test phones. Sign in, permit notifications and enable availability on approved doctor accounts. On Android 14+, permit full-screen alerts if desired; heads-up notifications remain the fallback. Notification channel settings and Do Not Disturb still apply.
6. Check device registration and queue status using aggregate queries. Never expose FCM tokens in reports. No broadcast price should be added as part of this activation.

Rollback: disable `consultation-notification-worker` in pg_cron and make doctors unavailable. This leaves the independent `appointment-cron-job` untouched.

## Important gaps before real consultations

- The deployed claim RPC creates `consultation_sessions.status = 'pending'`. Nothing currently provisions that Stream call. The new UI therefore says accepted/video setup pending and does not enter the old unrestricted call-creation path.
- Add a trusted, idempotent Stream provisioner that sets exactly the patient Auth ID and winning doctor Auth ID as members, uses a restricted call type, and marks the session ready only after confirmed creation. Update token issuance to verify participation in that request; preserve the scheduled appointment token contract separately.
- Add an authorized completion/failure RPC and provider/webhook cleanup that transitions claimed requests, closes sessions and releases `doctor_consultation_locks`. Currently a claimed request blocks that patient's next request indefinitely, and the doctor lock has a two-hour safety lease. Use designated test accounts until this lifecycle is complete; do not manually mark real consultations completed just to bypass it.
- Add database tests for simultaneous claims, concurrent creation with different idempotency keys, role isolation and stale outbox leases. Client tests do not prove PostgreSQL concurrency or RLS correctness.
- Decide broadcast pricing later. Private creation currently snapshots `doctors.hourly_rate` as the request fee. Confirm that product meaning/duration before charging anyone. This implementation collects no payment.

## Verification checklist

Automated: Android assembleDebug, unit tests and lint; Deno strict type check and worker tests covering multi-device success, partial failure, invalid-token cleanup, expired invitations, no devices, close events, payload allowlisting, malformed data, execution budget and error redaction.

Requires real devices and a deployed worker: private invitation; two eligible doctors racing on a broadcast (after a separately approved tariff); first claimant wins; losing and winning doctor's extra devices stop; patient cancellation; decline; expiry; lock-screen/background behavior; notification denial; delayed/duplicate push; offline/reconnect; logout/account switch; token refresh; existing scheduled call notification and joining. No physical device/emulator was connected during implementation.

Android background behavior references: https://firebase.google.com/docs/cloud-messaging/android-message-priority and https://developer.android.com/develop/background-work/services/fgs/timeout.
