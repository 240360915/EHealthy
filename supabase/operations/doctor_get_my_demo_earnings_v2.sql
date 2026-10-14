
-- Doctor-only unified virtual earnings. Source IDs are tagged as scheduled
-- appointment or on-demand request to avoid confusing the two entity types.
CREATE OR REPLACE FUNCTION public.doctor_get_my_demo_earnings_v2()
RETURNS TABLE(
    source_id uuid,source_type text,appointment_date text,
    payment_reference text,total_minor integer,doctor_minor integer,
    platform_minor integer,commission_bps integer,released_at text
)
LANGUAGE plpgsql SECURITY DEFINER
SET search_path TO 'pg_catalog','public'
AS $function$
BEGIN
  IF auth.uid() IS NULL OR NOT EXISTS
      (SELECT 1 FROM public.doctors d WHERE d.user_id=auth.uid()) THEN
    RAISE EXCEPTION 'Doctor account required';
  END IF;
  RETURN QUERY
  SELECT e.appointment_id, 'scheduled_online'::text,a.date::text,p.payment_ref,
         e.total_minor,e.doctor_minor,e.platform_minor,e.commission_bps,
         e.released_at::text
    FROM public.demo_earnings e
    JOIN public.appointments a ON a.id=e.appointment_id
    JOIN public.payments p ON p.id=e.payment_id
    JOIN public.doctors d ON d.id=e.doctor_id
    WHERE d.user_id=auth.uid()
  UNION ALL
  SELECT e.consultation_request_id,
         p.payment_type::text,(cr.claimed_at AT TIME ZONE 'Africa/Johannesburg')::date::text,
         p.payment_ref,e.total_minor,e.doctor_minor,e.platform_minor,e.commission_bps,
         e.released_at::text
    FROM public.demo_on_demand_earnings e
    JOIN public.consultation_requests cr ON cr.id=e.consultation_request_id
    JOIN public.payments p ON p.id=e.payment_id
    JOIN public.doctors d ON d.id=e.doctor_id
    WHERE d.user_id=auth.uid()
  ORDER BY 9 DESC;
END;
$function$;
REVOKE ALL ON FUNCTION public.doctor_get_my_demo_earnings_v2() FROM PUBLIC,anon;
GRANT EXECUTE ON FUNCTION public.doctor_get_my_demo_earnings_v2() TO authenticated;
