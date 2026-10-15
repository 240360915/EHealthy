-- DemoPay earnings are released only after a verified ended call.
-- Ownership, payment, idempotency and commission checks remain unchanged.
CREATE OR REPLACE FUNCTION public.doctor_get_my_demo_ready_for_release()
 RETURNS TABLE(appointment_id uuid, patient_name text, appointment_date text, appointment_time text, total_minor integer)
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'pg_catalog', 'public'
AS $function$
begin
  if auth.uid() is null then
    raise exception 'Authentication required';
  end if;
  if not exists (select 1 from public.doctors d where d.user_id = auth.uid()) then
    raise exception 'Doctor profile not found';
  end if;
  return query
    select a.id, a.patient_name, a.date::text, a.time::text,
           a.price_minor
    from public.appointments a
    join public.doctors d on d.id = a.doctor_id
    where d.user_id = auth.uid()
      and a.status = 'confirmed'
      and a.appointment_type = 'online'
      and a.call_joined_doctor_at is not null
      and a.call_joined_patient_at is not null
      and a.call_ended_at is not null
      and coalesce(a.funds_released, false) = false
      and exists (
        select 1 from public.payments p
        where p.appointment_id = a.id and p.doctor_id = d.id
          and p.gateway_provider = 'ehealthy_demo'
          and p.payment_type = 'scheduled_online'
          and p.status = 'paid' and p.currency = 'ZAR'
      )
      and not exists (select 1 from public.demo_earnings e
                      where e.appointment_id = a.id)
    order by a.date desc, a.time desc;
end;
$function$;

CREATE OR REPLACE FUNCTION public.doctor_complete_online_demo_appointment(p_appointment_id uuid)
 RETURNS TABLE(appointment_id uuid, appointment_status text, total_minor integer, doctor_minor integer, platform_minor integer, commission_bps integer)
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'pg_catalog', 'public'
AS $function$
declare
  v_doc_id uuid;
  v_appt public.appointments%rowtype;
  v_payment public.payments%rowtype;
  v_existing public.demo_earnings%rowtype;
  v_bps integer;
  v_total integer;
  v_platform integer;
  v_doctor integer;
begin
  if auth.uid() is null then
    raise exception 'Sign in as a doctor first';
  end if;
  select d.id into v_doc_id
    from public.doctors d where d.user_id = auth.uid() limit 1;
  if v_doc_id is null then
    raise exception 'Doctor profile not found';
  end if;

  select a.* into v_appt from public.appointments a
    where a.id = p_appointment_id and a.doctor_id = v_doc_id
    for update;
  if not found then
    raise exception 'Appointment not found or access denied';
  end if;
  if v_appt.appointment_type <> 'online' then
    raise exception 'Only scheduled online DemoPay consultations are supported';
  end if;

  -- Idempotent response: a previous successful release is not repeated.
  select e.* into v_existing from public.demo_earnings e
    where e.appointment_id = v_appt.id;
  if found then
    if v_appt.status <> 'completed' then
      raise exception 'Appointment and virtual earnings states are inconsistent';
    end if;
    return query select v_existing.appointment_id, 'completed'::text,
      v_existing.total_minor, v_existing.doctor_minor,
      v_existing.platform_minor, v_existing.commission_bps;
    return;
  end if;

  if v_appt.status <> 'confirmed' then
    raise exception 'Only confirmed appointments can be completed';
  end if;
  if coalesce(v_appt.funds_released, false) then
    raise exception 'Virtual release already flagged without an earnings record; contact administrator';
  end if;
  if v_appt.call_joined_patient_at is null or v_appt.call_joined_doctor_at is null then
    raise exception 'Both doctor and patient must join the scheduled video call before completion';
  end if;
  if v_appt.call_ended_at is null then
    raise exception 'End the scheduled video consultation before recording completion and demo earnings';
  end if;

  select p.* into v_payment
  from public.payments p
  where p.appointment_id = v_appt.id
    and p.doctor_id = v_doc_id
    and p.status = 'paid'
    and p.gateway_provider = 'ehealthy_demo'
    and p.payment_type = 'scheduled_online'
    and p.currency = 'ZAR'
  order by p.created_at desc
  limit 1
  for update;
  if not found then
    raise exception 'No successful scheduled DemoPay payment exists for this appointment';
  end if;
  if v_payment.amount is null or v_payment.amount <= 0 then
    raise exception 'Invalid simulated payment amount';
  end if;

  v_total := round(v_payment.amount * 100)::integer;
  if v_appt.price_minor is null or v_total <> v_appt.price_minor then
    raise exception 'Appointment price and simulated payment amount differ';
  end if;
  select s.commission_bps into v_bps
    from public.demo_platform_settings s where s.singleton = true;
  if v_bps is null then
    raise exception 'EHealthy demo commission settings are missing';
  end if;

  v_platform := round(v_total::numeric * v_bps / 10000)::integer;
  v_doctor := v_total - v_platform;

  insert into public.demo_earnings (
    appointment_id, payment_id, doctor_id, currency, total_minor,
    commission_bps, platform_minor, doctor_minor
  ) values (
    v_appt.id, v_payment.id, v_doc_id, 'ZAR', v_total,
    v_bps, v_platform, v_doctor
  );

  update public.appointments a
  set status = 'completed', funds_released = true
  where a.id = v_appt.id and a.doctor_id = v_doc_id;

  return query select v_appt.id, 'completed'::text, v_total,
    v_doctor, v_platform, v_bps;
end;
$function$;
