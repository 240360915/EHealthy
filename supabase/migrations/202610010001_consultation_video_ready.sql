begin;

-- The claim RPC owns session creation. App users must never choose a room or its state.
alter table public.consultation_sessions enable row level security;
revoke insert, update, delete, truncate, references, trigger
  on public.consultation_sessions from anon, authenticated;

create or replace function public.mark_consultation_video_ready(
  p_request_id uuid, p_call_id text, p_patient_id uuid, p_doctor_id uuid
) returns void
language plpgsql security definer
set search_path = pg_catalog, public
as $$
declare
  r public.consultation_requests%rowtype;
  s public.consultation_sessions%rowtype;
begin
  -- Service role only; never callable by an authenticated phone.
  if coalesce(auth.role(), '') <> 'service_role' then
    raise exception 'Service role required' using errcode = '42501';
  end if;
  select * into r from public.consultation_requests where id = p_request_id for update;
  if not found or r.status <> 'claimed'
      or r.patient_id is distinct from p_patient_id
      or r.claimed_doctor_id is distinct from p_doctor_id then
    raise exception 'Consultation changed';
  end if;
  select * into s from public.consultation_sessions where request_id = p_request_id for update;
  if not found or s.status not in ('pending', 'ready')
      or s.stream_call_id is distinct from p_call_id
      or p_call_id is distinct from ('consultation-' || p_request_id::text) then
    raise exception 'Session unavailable';
  end if;
  update public.consultation_sessions
    set status = 'ready', ready_at = coalesce(ready_at, now())
    where request_id = p_request_id;
end;
$$;
revoke all on function public.mark_consultation_video_ready(uuid, text, uuid, uuid) from public, anon, authenticated;
grant execute on function public.mark_consultation_video_ready(uuid, text, uuid, uuid) to service_role;
commit;
