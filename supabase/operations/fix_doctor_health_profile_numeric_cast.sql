-- EHealthy doctor patient-care profile type fix (2026-10-15).
-- Supabase patients.height_cm/weight_kg are NUMERIC, but the existing
-- RETURNS TABLE contract declares DOUBLE PRECISION. PL/pgSQL RETURN QUERY
-- requires explicit casts to avoid "structure of query does not match".
-- Function retains its existing doctor-appointment access check.
-- Applied to live Supabase; keep this file for repeatable deployment.

CREATE OR REPLACE FUNCTION public.get_patient_health_profile_for_doctor(p_appointment_id uuid)
 RETURNS TABLE(id uuid, name text, surname text, gender text, date_of_birth text, blood_group text, allergies text, chronic text, medication text, surgeries text, disability text, emergency_contact_name text, emergency_contact_phone text, emergency_contact_relationship text, preferred_language text, smoking_status text, alcohol_use text, height_cm double precision, weight_kg double precision, current_symptoms text, symptom_duration text, symptom_severity integer, family_medical_history text, health_profile_updated_at text)
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
    v_doctor_id uuid;
    v_patient_id uuid;
begin

    select d.id
    into v_doctor_id
    from public.doctors d
    where d.user_id = auth.uid()
    limit 1;

    if v_doctor_id is null then
        raise exception 'Doctor profile could not be found.';
    end if;

    select a.patient_id
    into v_patient_id
    from public.appointments a
    where a.id = p_appointment_id
      and a.doctor_id = v_doctor_id
    limit 1;

    if v_patient_id is null then
        raise exception 'You do not have access to this patient.';
    end if;

    return query
    select
        p.id,
        p.name,
        p.surname,
        p.gender,
        p.date_of_birth::text,
        p.blood_group,
        p.allergies,
        p.chronic,
        p.medication,
        p.surgeries,
        p.disability,
        p.emergency_contact_name,
        p.emergency_contact_phone,
        p.emergency_contact_relationship,
        p.preferred_language,
        p.smoking_status,
        p.alcohol_use,
        p.height_cm::double precision,
        p.weight_kg::double precision,
        p.current_symptoms,
        p.symptom_duration,
        p.symptom_severity,
        p.family_medical_history,
        p.health_profile_updated_at::text
    from public.patients p
    where p.id = v_patient_id;

end;
$function$;
