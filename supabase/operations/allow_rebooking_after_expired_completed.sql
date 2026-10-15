-- EHealthy: allow rebooking after an appointment ends, fails, or expires.
-- Retain the existing rule of one ACTIVE appointment per patient per day.
-- A conflicting pending/confirmed/in-progress appointment still blocks a new booking.
create or replace function public.enforce_one_appointment_per_patient_day()
returns trigger
language plpgsql
security definer
set search_path to 'pg_catalog', 'public'
as $function$
begin
  if new.patient_id is null or new.date is null then
    return new;
  end if;

  -- Completed, expired, cancelled, failed or otherwise terminal appointments
  -- must not occupy a patient's booking day.
  if lower(btrim(coalesce(new.status, ''))) not in (
    'pending', 'confirmed', 'accepted', 'approved',
    'scheduled', 'rescheduled', 'in_progress'
  ) then
    return new;
  end if;

  -- Serialize concurrent bookings for this patient.
  perform 1
  from public.patients p
  where p.id = new.patient_id
  for update;

  if exists (
    select 1
    from public.appointments a
    where a.patient_id = new.patient_id
      and a.date = new.date
      and a.id is distinct from new.id
      and lower(btrim(coalesce(a.status, ''))) in (
        'pending', 'confirmed', 'accepted', 'approved',
        'scheduled', 'rescheduled', 'in_progress'
      )
  ) then
    raise exception
      'You already have an active appointment on %. Only one active appointment per day is allowed.',
      new.date;
  end if;

  return new;
end;
$function$;
