
-- DemoPay pending bookings must not indefinitely reserve the patient's day.
-- Thirty minutes without doctor confirmation expires the pending booking.
-- The 5-minute cron also expires past appointments while preserving started calls.
-- Expired paid bookings are flagged for separate simulated refund review.
CREATE OR REPLACE FUNCTION public.expire_past_appointments()
RETURNS void LANGUAGE plpgsql SECURITY DEFINER
SET search_path TO 'pg_catalog','public'
AS $function$
BEGIN
  WITH expired AS (
    UPDATE public.appointments a SET status='expired'
    WHERE lower(coalesce(a.status,'')) IN ('pending','confirmed','accepted')
      AND a.call_started_at IS NULL
      AND a.call_joined_patient_at IS NULL
      AND a.call_joined_doctor_at IS NULL
      AND (
        (a.date IS NOT NULL AND a.time IS NOT NULL
          AND (a.date + a.time::time) < (now() AT TIME ZONE 'Africa/Johannesburg'))
        OR
        (lower(a.status)='pending'
          AND a.payment_method='demo_card'
          AND a.created_at <= now() - interval '30 minutes')
      )
    RETURNING a.slot_id
  )
  UPDATE public.time_slots ts SET is_booked=false
   WHERE ts.id IN (SELECT x.slot_id FROM expired x WHERE x.slot_id IS NOT NULL)
     AND NOT EXISTS(
        SELECT 1 FROM public.appointments active
         WHERE active.slot_id=ts.id
           AND lower(coalesce(active.status,'')) IN
             ('pending','confirmed','accepted','approved','scheduled','rescheduled','in_progress')
     );
END;
$function$;
