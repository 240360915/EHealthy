-- Doctor-only physical follow-up history. Separate request and invoice
-- records exactly as the patient workflow expects.
CREATE OR REPLACE FUNCTION public.doctor_get_my_physical_visit_history()
RETURNS SETOF public.physical_visit_requests
LANGUAGE sql STABLE SECURITY DEFINER
SET search_path TO 'pg_catalog', 'public'
AS $$
    SELECT r.* FROM public.physical_visit_requests r
    INNER JOIN public.doctors d ON d.id = r.doctor_id
    WHERE auth.uid() IS NOT NULL
      AND d.user_id = auth.uid()
      AND coalesce(d.is_deactivated,false)=false
    ORDER BY r.requested_at DESC;
$$;
CREATE OR REPLACE FUNCTION public.doctor_get_my_physical_invoice_history()
RETURNS SETOF public.in_person_invoices
LANGUAGE sql STABLE SECURITY DEFINER
SET search_path TO 'pg_catalog', 'public'
AS $$
    SELECT i.* FROM public.in_person_invoices i
    INNER JOIN public.doctors d ON d.id=i.doctor_id
    WHERE auth.uid() IS NOT NULL
      AND d.user_id=auth.uid()
      AND coalesce(d.is_deactivated,false)=false
    ORDER BY i.issued_at DESC;
$$;
REVOKE ALL ON FUNCTION public.doctor_get_my_physical_visit_history() FROM PUBLIC,anon;
REVOKE ALL ON FUNCTION public.doctor_get_my_physical_invoice_history() FROM PUBLIC,anon;
GRANT EXECUTE ON FUNCTION public.doctor_get_my_physical_visit_history() TO authenticated;
GRANT EXECUTE ON FUNCTION public.doctor_get_my_physical_invoice_history() TO authenticated;