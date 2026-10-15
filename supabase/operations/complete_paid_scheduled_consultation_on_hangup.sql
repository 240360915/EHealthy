-- Do not backfill previously cancelled bookings; they require payment/refund review.
-- The function handles future hang-ups and repeated hang-ups safely.
CREATE OR REPLACE FUNCTION public.end_scheduled_call(p_appointment_id uuid)
RETURNS timestamptz
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'pg_catalog', 'public'
AS $function$
DECLARE
  v_actor uuid := auth.uid();
  v_appt public.appointments%rowtype;
  v_end timestamptz;
  v_paid boolean := false;
BEGIN
  IF v_actor IS NULL THEN
    RAISE EXCEPTION 'Authentication required';
  END IF;

  SELECT a.* INTO v_appt
    FROM public.appointments a
    LEFT JOIN public.patients p ON p.id = a.patient_id
    LEFT JOIN public.doctors d ON d.id = a.doctor_id
   WHERE a.id = p_appointment_id
     AND a.appointment_type = 'online'
     AND (p.user_id = v_actor OR d.user_id = v_actor)
   FOR UPDATE OF a;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'Appointment not found or access denied';
  END IF;
  -- Both phones can hang up without double-counting a visit.
  IF v_appt.status = 'completed' AND v_appt.call_ended_at IS NOT NULL THEN
    RETURN v_appt.call_ended_at;
  END IF;
  IF v_appt.status <> 'confirmed' THEN
    RAISE EXCEPTION 'Consultation is not confirmed or has already been cancelled';
  END IF;

  UPDATE public.appointments a
     SET call_ended_at = coalesce(a.call_ended_at, now()),
         call_ended_by = coalesce(a.call_ended_by, v_actor)
   WHERE a.id = p_appointment_id
   RETURNING a.call_ended_at INTO v_end;

  -- A one-sided abandoned call is NOT automatically a paid completion.
  IF v_appt.call_joined_patient_at IS NOT NULL
    AND v_appt.call_joined_doctor_at IS NOT NULL
    AND v_appt.call_started_at IS NOT NULL
  THEN
    SELECT EXISTS(
      SELECT 1 FROM public.payments pay
       WHERE pay.appointment_id = v_appt.id
         AND pay.doctor_id = v_appt.doctor_id
         AND pay.patient_id = v_appt.patient_id
         AND pay.gateway_provider = 'ehealthy_demo'
         AND pay.payment_type = 'scheduled_online'
         AND pay.status = 'paid'
         AND pay.currency = 'ZAR'
         AND v_appt.price_minor > 0
         AND round(pay.amount * 100)::integer = v_appt.price_minor
    ) INTO v_paid;

    IF v_paid THEN
      -- Existing AFTER UPDATE status trigger records exactly one demo earning.
      UPDATE public.appointments a
         SET status = 'completed', funds_released = true
       WHERE a.id = p_appointment_id;
    END IF;
  END IF;
  RETURN v_end;
END;
$function$;

-- Only an authenticated participant can call this SECURITY DEFINER RPC.
REVOKE ALL ON FUNCTION public.end_scheduled_call(uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.end_scheduled_call(uuid) TO authenticated;
