-- Return only this doctor's saved, verified practice address for invoice UI.
-- Canonical doctor_create_physical_visit_invoice always copies this saved map pin.
CREATE OR REPLACE FUNCTION public.doctor_get_my_invoice_location()
RETURNS TABLE(location_name text, location_address text, has_pin boolean)
LANGUAGE sql STABLE SECURITY DEFINER
SET search_path TO 'pg_catalog','public'
AS $$
 SELECT d.practice_location_name, d.practice_address,
        (d.lat IS NOT NULL AND d.lng IS NOT NULL
          AND length(trim(coalesce(d.practice_address,'')))>=3) AS has_pin
 FROM public.doctors d
 WHERE d.user_id=auth.uid() AND auth.uid() IS NOT NULL
   AND coalesce(d.is_deactivated,false)=false
 LIMIT 1;
$$;
REVOKE ALL ON FUNCTION public.doctor_get_my_invoice_location() FROM PUBLIC,anon;
GRANT EXECUTE ON FUNCTION public.doctor_get_my_invoice_location() TO authenticated;