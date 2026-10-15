-- Keep actionable follow-ups on the doctor dashboard after acceptance
-- until the doctor issues an in-person invoice.
CREATE OR REPLACE FUNCTION public.doctor_get_my_physical_visit_requests()
RETURNS SETOF public.physical_visit_requests
LANGUAGE sql STABLE SECURITY DEFINER
SET search_path TO 'pg_catalog', 'public'
AS $function$
  SELECT r.* FROM public.physical_visit_requests r
  JOIN public.doctors d ON d.id=r.doctor_id
  WHERE d.user_id=auth.uid() AND coalesce(d.is_deactivated,false)=false
    AND (r.status='pending' OR
      (r.status='accepted' AND NOT EXISTS(
        SELECT 1 FROM public.in_person_invoices i WHERE i.request_id=r.id
      )))
  ORDER BY CASE WHEN r.status='pending' THEN 0 ELSE 1 END,
    r.requested_at DESC;
$function$;
REVOKE ALL ON FUNCTION public.doctor_get_my_physical_visit_requests() FROM PUBLIC,anon;
GRANT EXECUTE ON FUNCTION public.doctor_get_my_physical_visit_requests() TO authenticated;
