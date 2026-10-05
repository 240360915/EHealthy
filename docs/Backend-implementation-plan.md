# eHealth Connect: backend work required before live booking and broadcast calls

Status: design only. No SQL, policies, functions, deployments or production data were changed in this work session.

## Decisions and limits

- Authentication is `auth.users.id`. Profile ownership is `patients.user_id` / `doctors.user_id`. Appointment, message, prescription, review and slot relationships use profile `id` values.
- The Android call demo uses synthetic doctors and in-memory state. R150 is an illustrative demo price, not an approved tariff. It sends no FCM messages, creates no Stream call and writes no consultation/payment records.
- Private requests target one selected doctor. Broadcast requests target all eligible doctors in the chosen service scope. “All” should mean approved, active, opted-in, recently online, licensed for the requested service and not occupied. It should not mean every registered doctor.
- Calling this an emergency service would promise more than the system delivers. Use “on-demand consultation,” state that an answer is not guaranteed, and agree the clinical escalation process before launch.
- For the prototype both modes show the same illustrative fee. Decide whether private consultations use the standard tariff or the selected doctor's fee before production. Store a server-generated price snapshot with every request.

## 1. Audit and identity migration

1. Export schema, policies, constraints and a recoverable backup. Confirm the prior audit is still current; the cached audit reported 71 profile/auth-equal slots and 11 auth-ID-only slots.
2. Verify every non-null `user_id` has exactly one matching auth user, no duplicate ownership rows exist, and the app's two-role policy is intentional. Add unique constraints on each profile table's `user_id` after resolving duplicates. Add ownership foreign keys and nullability only after the audit.
3. For slots, migrate only records with **no existing doctor profile ID match** and exactly one `doctors.user_id` match. Do not blindly update every `doctor_id = user_id`: a UUID could already identify another valid profile. Audit such cross-profile collisions separately. Take write locks or use a maintenance window so the mapping and duplicate checks cannot change mid-migration.
4. Check duplicate `(doctor_id,date,time)` tuples after the proposed mapping; inspect booked rows and appointment references. Fix ambiguities manually, then add the foreign key to `doctors.id` and a unique constraint on `(doctor_id,date,time)`.
5. Audit appointments and dependent tables similarly. Never assume that the prior patient profile/auth equality remains true.
6. Update RLS consistently using profile ownership `EXISTS` checks. Verify reads/inserts/updates/deletes separately using two distinct patients and two doctors. A client-side profile lookup is not access control.

## 2. Secure slots, booking and rescheduling

Define `reserve_slot(p_slot_id uuid, p_appointment_type text, p_reason text, p_idempotency_key uuid)` returning the appointment ID and authoritative reservation/payment state.

- Resolve the patient from `auth.uid()`; reject missing, duplicate or disabled accounts. Do not accept a patient ID or price from the client.
- Lock the requested slot with `SELECT ... FOR UPDATE`. Verify the doctor remains approved and active, the slot belongs to that doctor, it is unbooked, and its time is in the future in the agreed practice timezone (initially Africa/Johannesburg).
- Derive the price and currency on the server, in integer minor units. Insert the appointment with an unpaid status, associate `slot_id`, mark the slot booked, and commit together.
- Add unique active-slot ownership and `(patient_id,idempotency_key)` constraints. A repeated request returns the original result; a key reused for different inputs fails.
- Implement `reschedule_appointment(p_appointment_id,p_new_slot_id,p_reason,p_idempotency_key)` as one transaction. Verify ownership and allowed appointment status; lock old and new slots in a consistent ID order; claim the new slot, release the old slot, and update the appointment atomically. Failure leaves the old reservation intact.
- Implement `cancel_appointment` to release its slot transactionally according to cancellation rules. Existing Android cancellation currently updates appointment status directly; replace this path before enabling reservation functions.
- Implement `publish_availability` with a unique constraint and atomic add/remove operations. Never allow an availability edit to clear `is_booked` or delete a booked slot. Current Android editing preserves existing slot rows but cannot guarantee multi-client publication atomicity.
- Do not enable the Android booking/reschedule confirmation paths until these functions are deployed and tested. They currently display an explicit server-support requirement and make no reservation/payment writes.

## 3. Proposed consultation tables

These are proposed additions; reconcile names with the live schema before writing a migration.

| Table | Fields and constraints |
| --- | --- |
| `consultation_prices` | service/mode, currency, positive `amount_minor`, effective dates, enabled; admin/server writes only |
| `doctor_presence` | doctor profile PK/FK, opt-in available flag, server timestamp heartbeat, supported service scope; owner can change opt-in only through a validated function |
| `consultation_requests` | UUID PK, patient profile FK, mode private/broadcast, optional target doctor FK, status, price/currency snapshot, created/expiry times, nullable claimed doctor FK, version counter, idempotency key; checks for mode/target consistency and claimed-state fields |
| `consultation_recipients` | request FK + doctor FK composite PK, pending/declined/closed state, notification metadata; only server creates recipients |
| `doctor_consultation_locks` | doctor profile PK, unique request FK, lease expiry; enforces one active consultation per doctor across different requests |
| `consultation_sessions` | unique request FK, unique Stream call ID, provisioning state, timestamps; created after successful claim |
| `notification_outbox` | event UUID, request/version, recipient doctor, event type, retry state, creation time; deduplicate on event/recipient and retain failures for inspection |
| `device_tokens` | owner auth ID, installation ID, token and last-seen time; owner/server access only; supports multiple devices and logout cleanup |

Use RLS on every table. Patients read their requests; recipient doctors read only the invitation/status necessary to decide; only the winning doctor gains consultation details. Keep clinical reasons and contact details out of broadcast notifications and broad recipient reads. Raw table writes to request status, price, winner, sessions and outbox are denied to app users.

## 4. RPC contracts and the acceptance race

Use authenticated functions with explicit authorization, fixed `search_path`, fully qualified table names, narrow grants, and no public/anonymous execution. A `SECURITY DEFINER` function must still verify `auth.uid()` itself.

- `create_consultation_request(mode,target_doctor_id,service,idempotency_key)`: validate patient, limits and scope; resolve a server tariff; create request/eligible-recipient snapshot/outbox events in one transaction. Private mode creates at most one recipient. No candidates returns `no_doctors`, without ringing. Prevent duplicate active requests for the same patient.
- `claim_consultation_request(request_id,idempotency_key)`: derive doctor profile from auth; lock the request row; check pending status, server expiry, recipient membership, approval, deactivation, fresh presence and service eligibility. Atomically obtain the unique doctor occupancy lock; an occupied doctor cannot claim a second request. Update the winner and request version and enqueue recipient-close events in the same transaction. Repeated acceptance by the winner returns the same result; another doctor gets `already_claimed`.
- `decline_consultation_request(request_id)`: mark only the caller's invitation declined. It must not cancel everyone else's invitation. All declines produce a terminal no-answer state and close notifications.
- `cancel_consultation_request(request_id)`: require patient ownership, lock and cancel only a pending request. If already claimed, apply a separately defined cancellation policy; do not undo the winner silently.
- `get_consultation_request(request_id)`: authorize patient or recipient; return server state/version and only permitted fields.
- `expire_consultation_requests`: server scheduler transitions expired pending requests and enqueues close events. Claim also checks expiry, so correctness does not depend on timely cron execution.

The claim's linearization point is the committed database transaction. The first **successful server claim** wins, not necessarily the first tap or push receipt. PostgreSQL row locks block competing updates until that transaction completes. [PostgreSQL row-lock documentation](https://www.postgresql.org/docs/17/explicit-locking.html).

## 5. FCM and stop-ringing behavior

Reuse the existing `appointment-cron` FCM signing/sending approach through a dedicated outbox worker. Do not put service credentials or FCM send authority in Android.

Payload: `type=consultation_invite|consultation_closed`, `request_id`, monotonically increasing `version`, `expires_at`; no medical reason, tokens, patient name or card details. Use per-request notification identities and an expiry TTL. A delivery receipt is not an acceptance.

Android must fetch authorized server state before ringing and before accepting. Discard expired or older versions; persist the highest terminal version so an old invitation cannot restart ringing. On claim/cancel/expiry, publish the new state through Supabase Realtime and enqueue FCM close events. Clear the matching notification, sound and incoming-call screen. Reconcile on app resume/reconnect and use a local timeout: notifications can be delayed or lost, so instant silence on an offline phone cannot be guaranteed. [FCM message lifespan](https://firebase.google.com/docs/cloud-messaging/customize-messages/setting-message-lifespan).

The current Android Firebase Google Services plugin is commented out. Verify the application ID, Firebase configuration, notification permission, background delivery and multiple-device logout/token revocation before enabling real alerts. Do not reuse the existing fixed notification ID 9999 for broadcast requests.

## 6. Stream session provisioning

After the claim commits, an idempotent server worker creates one private Stream session containing only the patient and winning doctor's auth IDs. It records the call ID in `consultation_sessions`. The other recipients never become call members.

Extend the token endpoint to require a request/appointment ID and verify participation and allowed status. Configure the Stream call type so only authorized members can join and ordinary users cannot create arbitrary consultations. Token generation alone is not membership enforcement. The present app joins `default` calls with `create=true`, and the existing `stream-token` endpoint authenticates the user without checking appointment membership; review both before production. Keep the existing scheduled-call implementation separate while introducing the new flow. [Stream permissions](https://getstream.io/video/docs/api/call-types/permissions/), [server-side calls](https://getstream.io/video/docs/api/calls/).

Provisioning failure must show a retryable “setting up consultation” or failed state, never a fake joined call. Use bounded retries and a recovery/cancellation policy that releases occupancy safely. No new winner is assigned after a committed claim without an explicit state transition and participant notification.

## 7. Payments and outstanding security

- Real payments need hosted/tokenized checkout and verified, idempotent provider webhooks. The server alone sets paid/refunded/subscription/funds-released fields. Capture/refund rules must be agreed for no-answer, cancellation, call setup failure and no-shows.
- Audit existing `card_number`/`card_expiry` data and the web payment UI separately. The Android card form is removed; this does not clean historical data or fix the web app.
- Recheck private doctor documents, prescription policies and receipt policies identified in the prior conversation. The source does not establish whether earlier suggested SQL was ever executed.
- Move completion-code generation/verification and funds-release actions from client updates into protected functions; current legacy paths remain and must not control real money.

## 8. Deployment acceptance tests

Before enabling the live feature, test two real doctor accounts on separate devices and a patient account:

1. Simultaneous claims: exactly one winner; both devices receive authoritative closure; the loser cannot obtain call access.
2. One doctor accepting two patient requests simultaneously: at most one active occupancy.
3. Private request reaches only the selected eligible doctor; unapproved, unavailable, deactivated and stale-heartbeat doctors cannot claim.
4. Repeat network submissions are idempotent; old request IDs and reused keys with changed payloads fail.
5. Acceptance racing cancellation/expiry has one consistent terminal result.
6. Duplicate/out-of-order invitations after closure do not ring again; offline/background/restarted apps reconcile safely.
7. Crash after DB commit but before push/Stream creation is recovered by the outbox/provisioning worker.
8. RLS denies cross-patient reads and unauthorized price/winner/payment changes.
9. Two bookings for one slot yield one reservation; failed rescheduling preserves the old slot.
10. Payment webhook replay, setup failure and no-answer do not produce duplicate charges or fake refunds.

Only after these pass: enable Android RPC-backed booking and real consultation requests, replace demo pricing, and retain the demo as a separate development-only feature or remove it from release builds.
