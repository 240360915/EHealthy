-- The doctor controls one stable practice map pin, as on Doctors-Dashboard.html.
-- Only the owner may edit/read their draft pin. Patients may view it ONLY
-- after that doctor accepts their own physical follow-up request.
CREATE OR REPLACE FUNCTION public.doctor_get_my_editable_practice_location()
RETURNS TABLE (
  location_name text,
  location_address text,
  latitude double precision,
  longitude double precision
)
LANGUAGE sql STABLE SECURITY DEFINER
SET search_path TO 'pg_catalog', 'public'
AS $$
  SELECT d.practice_location_name, d.practice_address, d.lat, d.lng
  FROM public.doctors d
  WHERE auth.uid() IS NOT NULL AND d.user_id = auth.uid()
    AND coalesce(d.is_deactivated,false) = false
  LIMIT 1;
$$;

-- When an invoice exists prefer its COPIED map pin, not a later edit to
-- the doctor's default practice. Never expose locations for pending/declined
-- requests or requests belonging to another patient.
CREATE OR REPLACE FUNCTION public.patient_get_my_accepted_followup_locations()
RETURNS TABLE (
  request_id uuid, request_status text, request_reason text,
  practice_name text, practice_address text,
  latitude double precision, longitude double precision,
  invoice_id uuid, invoice_status text
)
LANGUAGE sql STABLE SECURITY DEFINER
SET search_path TO 'pg_catalog', 'public'
AS $$
  SELECT r.id, r.status, r.reason,
    COALESCE(NULLIF(i.location_name,''),d.practice_location_name),
    COALESCE(NULLIF(i.location,''),d.practice_address),
    COALESCE(i.location_latitude,d.lat),COALESCE(i.location_longitude,d.lng),
    i.id,i.status
  FROM public.physical_visit_requests r
  JOIN public.patients p ON p.id=r.patient_id
  JOIN public.doctors d ON d.id=r.doctor_id
  LEFT JOIN LATERAL (
    SELECT inv.* FROM public.in_person_invoices inv
    WHERE inv.request_id=r.id AND inv.patient_id=r.patient_id
    ORDER BY inv.issued_at DESC LIMIT 1
  ) i ON true
  WHERE auth.uid() IS NOT NULL AND p.user_id=auth.uid()
    AND r.status IN ('accepted','invoiced','patient_accepted')
  ORDER BY r.requested_at DESC;
$$;
REVOKE ALL ON FUNCTION public.doctor_get_my_editable_practice_location() FROM PUBLIC,anon;
REVOKE ALL ON FUNCTION public.patient_get_my_accepted_followup_locations() FROM PUBLIC,anon;
GRANT EXECUTE ON FUNCTION public.doctor_get_my_editable_practice_location() TO authenticated;
GRANT EXECUTE ON FUNCTION public.patient_get_my_accepted_followup_locations() TO authenticated;