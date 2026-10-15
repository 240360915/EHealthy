-- Scheduled and in-person completion codes, plus the doctor's pending follow-up inbox.
-- Never expose either privileged RPC to unauthenticated users.

CREATE OR REPLACE FUNCTION public.request_completion_code(p_appointment_id uuid)
RETURNS TABLE(appointment_id uuid, completion_code text, requested_at timestamptz)
LANGUAGE plpgsql SECURITY DEFINER
SET search_path TO 'pg_catalog', 'public'
AS $function$
DECLARE
  v_patient_id uuid;
  v_appointment public.appointments%rowtype;
  v_type text;
  v_number bigint;
  v_code text;
BEGIN
  IF auth.uid() IS NULL THEN RAISE EXCEPTION 'Authentication required'; END IF;
  SELECT p.id INTO v_patient_id FROM public.patients p WHERE p.user_id=auth.uid() LIMIT 1;
  IF v_patient_id IS NULL THEN RAISE EXCEPTION 'Patient profile not found'; END IF;
  SELECT a.* INTO v_appointment FROM public.appointments a
   WHERE a.id=p_appointment_id AND a.patient_id=v_patient_id FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'Appointment not found or access denied'; END IF;
  v_type := lower(coalesce(v_appointment.appointment_type,''));
  IF v_type NOT IN ('online','in_person') THEN RAISE EXCEPTION 'Unsupported appointment type'; END IF;
  -- Auto-completed paid video visits still need an optional 6-digit handoff to
  -- their doctor after hang-up. In-person visits still require confirmation.
  IF NOT (
      v_appointment.status='confirmed'
      OR (v_type='online' AND v_appointment.status='completed')
  ) THEN
    RAISE EXCEPTION 'This appointment cannot request a completion code';
  END IF;
  IF coalesce(v_appointment.completion_code_verified,false) THEN
    RAISE EXCEPTION 'This completion code has already been verified';
  END IF;
  IF v_type='online' AND (
      v_appointment.call_joined_patient_at IS NULL
      OR v_appointment.call_joined_doctor_at IS NULL
      OR v_appointment.call_ended_at IS NULL
  ) THEN RAISE EXCEPTION 'End the consultation with both people joined before generating a code'; END IF;
  IF nullif(trim(coalesce(v_appointment.completion_code,'')),'') IS NOT NULL THEN
    RETURN QUERY SELECT v_appointment.id,v_appointment.completion_code,
       v_appointment.completion_code_requested_at;
    RETURN;
  END IF;
  v_number := (('x' || encode(extensions.gen_random_bytes(4),'hex'))::bit(32)::bigint);
  v_code := (100000+(v_number%900000))::text;
  UPDATE public.appointments a SET completion_code=v_code,
    completion_code_requested_at=now(), completion_code_verified=false
  WHERE a.id=p_appointment_id;
  RETURN QUERY SELECT p_appointment_id,v_code,now();
END;
$function$;

CREATE OR REPLACE FUNCTION public.doctor_verify_appointment_completion_code(
  p_appointment_id uuid, p_code text)
RETURNS void
LANGUAGE plpgsql SECURITY DEFINER
SET search_path TO 'pg_catalog', 'public'
AS $function$
DECLARE
  v_a public.appointments%rowtype;
  v_d uuid;
BEGIN
  IF auth.uid() IS NULL THEN RAISE EXCEPTION 'Authentication required'; END IF;
  SELECT d.id INTO v_d FROM public.doctors d
   WHERE d.user_id=auth.uid() AND coalesce(d.is_deactivated,false)=false LIMIT 1;
  IF v_d IS NULL THEN RAISE EXCEPTION 'Doctor profile not found'; END IF;
  SELECT a.* INTO v_a FROM public.appointments a
   WHERE a.id=p_appointment_id AND a.doctor_id=v_d FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'Appointment not found or access denied'; END IF;
  IF v_a.status NOT IN ('confirmed','completed') THEN RAISE EXCEPTION 'Appointment is not eligible for completion verification'; END IF;
  IF v_a.completion_code IS NULL OR length(v_a.completion_code)<>6 THEN
    RAISE EXCEPTION 'The patient has not generated a completion code yet';
  END IF;
  IF p_code IS NULL OR p_code !~ '^[0-9]{6}$' OR p_code<>v_a.completion_code THEN
    RAISE EXCEPTION 'Incorrect 6-digit completion code';
  END IF;
  IF coalesce(v_a.completion_code_verified,false) THEN RETURN; END IF;
  IF v_a.appointment_type='online' THEN
    IF v_a.call_joined_patient_at IS NULL OR v_a.call_joined_doctor_at IS NULL
       OR v_a.call_ended_at IS NULL THEN
      RAISE EXCEPTION 'Finish the two-person online call before verifying the code';
    END IF;
    -- If not yet completed, use the existing paid-only completion RPC.
    IF v_a.status='confirmed' THEN
      PERFORM public.doctor_complete_online_demo_appointment(p_appointment_id);
    END IF;
    UPDATE public.appointments SET completion_code_verified=true WHERE id=v_a.id;
  ELSIF v_a.appointment_type='in_person' THEN
    IF v_a.status='confirmed' THEN
      UPDATE public.appointments SET status='completed',
        completion_code_verified=true WHERE id=v_a.id;
    ELSE
      UPDATE public.appointments SET completion_code_verified=true WHERE id=v_a.id;
    END IF;
  ELSE
    RAISE EXCEPTION 'Unsupported appointment type';
  END IF;
END;
$function$;

CREATE OR REPLACE FUNCTION public.doctor_get_my_physical_visit_requests()
RETURNS SETOF public.physical_visit_requests
LANGUAGE sql STABLE SECURITY DEFINER
SET search_path TO 'pg_catalog', 'public'
AS $function$
  SELECT r.* FROM public.physical_visit_requests r
  JOIN public.doctors d ON d.id=r.doctor_id
  WHERE d.user_id=auth.uid() AND coalesce(d.is_deactivated,false)=false
    AND r.status='pending'
  ORDER BY r.requested_at DESC;
$function$;

REVOKE ALL ON FUNCTION public.request_completion_code(uuid) FROM PUBLIC,anon;
REVOKE ALL ON FUNCTION public.doctor_verify_appointment_completion_code(uuid,text) FROM PUBLIC,anon;
REVOKE ALL ON FUNCTION public.doctor_get_my_physical_visit_requests() FROM PUBLIC,anon;
GRANT EXECUTE ON FUNCTION public.request_completion_code(uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION public.doctor_verify_appointment_completion_code(uuid,text) TO authenticated;
GRANT EXECUTE ON FUNCTION public.doctor_get_my_physical_visit_requests() TO authenticated;
