
-- EHealthy DemoPay: commission is earned only for a paid, completed service.
-- Existing scheduled ledger stays unchanged. On-demand gets a separate
-- idempotent ledger rather than overloading appointment_id with another entity.
CREATE TABLE IF NOT EXISTS public.demo_on_demand_earnings (
  consultation_request_id uuid PRIMARY KEY REFERENCES public.consultation_requests(id) ON DELETE RESTRICT,
  payment_id uuid NOT NULL UNIQUE REFERENCES public.payments(id) ON DELETE RESTRICT,
  doctor_id uuid NOT NULL REFERENCES public.doctors(id),
  currency text NOT NULL CHECK (currency = 'ZAR'),
  total_minor integer NOT NULL CHECK (total_minor > 0),
  commission_bps integer NOT NULL CHECK (commission_bps BETWEEN 0 AND 10000),
  platform_minor integer NOT NULL CHECK (platform_minor >= 0),
  doctor_minor integer NOT NULL CHECK (doctor_minor >= 0),
  released_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT demo_on_demand_amounts_match CHECK (total_minor = platform_minor + doctor_minor)
);
ALTER TABLE public.demo_on_demand_earnings ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.demo_on_demand_earnings FROM PUBLIC, anon, authenticated;

CREATE OR REPLACE FUNCTION public.record_completed_on_demand_demo_earnings()
RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER
SET search_path TO 'pg_catalog','public'
AS $function$
DECLARE
  v_payment public.payments%ROWTYPE;
  v_bps integer;
  v_platform integer;
BEGIN
  IF NEW.status <> 'completed'
     OR (TG_OP = 'UPDATE' AND OLD.status IS NOT DISTINCT FROM NEW.status)
  THEN
    RETURN NEW;
  END IF;

  IF NEW.claimed_doctor_id IS NULL THEN RETURN NEW; END IF;
  SELECT p.* INTO v_payment
    FROM public.payments p
    WHERE p.consultation_request_id = NEW.id
      AND p.doctor_id = NEW.claimed_doctor_id
      AND p.patient_id = NEW.patient_id
      AND p.gateway_provider = 'ehealthy_demo'
      AND p.payment_method = 'demo_card'
      AND p.payment_type = CASE WHEN NEW.mode = 'broadcast'
           THEN 'broadcast_consultation' ELSE 'private_consultation' END
      AND p.status = 'paid'
      AND p.currency = 'ZAR'
      AND round(p.amount * 100)::integer = NEW.price_minor
    ORDER BY p.created_at DESC LIMIT 1;
  IF NOT FOUND THEN RETURN NEW; END IF;

  SELECT s.commission_bps INTO v_bps
    FROM public.demo_platform_settings s WHERE singleton = true;
  IF v_bps IS NULL THEN RAISE EXCEPTION 'Demo commission setting missing'; END IF;
  v_platform := round(NEW.price_minor::numeric * v_bps / 10000)::integer;

  INSERT INTO public.demo_on_demand_earnings(
      consultation_request_id,payment_id,doctor_id,currency,total_minor,
      commission_bps,platform_minor,doctor_minor)
  VALUES(NEW.id,v_payment.id,NEW.claimed_doctor_id,'ZAR',
         NEW.price_minor,v_bps,v_platform,NEW.price_minor-v_platform)
  ON CONFLICT (consultation_request_id) DO NOTHING;
  RETURN NEW;
END;
$function$;

DROP TRIGGER IF EXISTS trg_record_completed_on_demand_demo_earnings ON public.consultation_requests;
CREATE TRIGGER trg_record_completed_on_demand_demo_earnings
AFTER UPDATE OF status ON public.consultation_requests
FOR EACH ROW
WHEN (NEW.status = 'completed' AND OLD.status IS DISTINCT FROM NEW.status)
EXECUTE FUNCTION public.record_completed_on_demand_demo_earnings();

-- Scheduled completion by a doctor's completion-code confirmation previously
-- set funds_released=true without writing to demo_earnings.
-- This trigger is idempotent with doctor_complete_online_demo_appointment().
CREATE OR REPLACE FUNCTION public.record_completed_scheduled_demo_earnings()
RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER
SET search_path TO 'pg_catalog','public'
AS $function$
DECLARE
  v_payment public.payments%ROWTYPE;
  v_bps integer;
  v_platform integer;
  v_total integer;
BEGIN
  IF NEW.status <> 'completed' OR NEW.appointment_type <> 'online'
     OR EXISTS(SELECT 1 FROM public.demo_earnings e WHERE e.appointment_id=NEW.id)
  THEN RETURN NEW; END IF;
  -- A mere label change is not clinical proof: require both joined and ended,
  -- or completion code verified (which itself enforces both joined and ended).
  IF NEW.call_joined_patient_at IS NULL OR NEW.call_joined_doctor_at IS NULL
     OR NEW.call_ended_at IS NULL THEN RETURN NEW; END IF;
  SELECT p.* INTO v_payment FROM public.payments p
   WHERE p.appointment_id=NEW.id AND p.doctor_id=NEW.doctor_id
     AND p.patient_id=NEW.patient_id
     AND p.gateway_provider='ehealthy_demo' AND p.payment_type='scheduled_online'
     AND p.status='paid' AND p.currency='ZAR'
   ORDER BY p.created_at DESC LIMIT 1;
  IF NOT FOUND THEN RETURN NEW; END IF;
  v_total := round(v_payment.amount * 100)::integer;
  IF v_total <= 0 OR v_total IS DISTINCT FROM NEW.price_minor THEN RETURN NEW; END IF;
  SELECT s.commission_bps INTO v_bps FROM public.demo_platform_settings s WHERE singleton=true;
  IF v_bps IS NULL THEN RAISE EXCEPTION 'Demo commission setting missing'; END IF;
  v_platform := round(v_total::numeric * v_bps / 10000)::integer;
  INSERT INTO public.demo_earnings(
     appointment_id,payment_id,doctor_id,currency,total_minor,
     commission_bps,platform_minor,doctor_minor)
  VALUES(NEW.id,v_payment.id,NEW.doctor_id,'ZAR',v_total,
         v_bps,v_platform,v_total-v_platform)
  ON CONFLICT (appointment_id) DO NOTHING;
  RETURN NEW;
END;
$function$;

DROP TRIGGER IF EXISTS trg_record_completed_scheduled_demo_earnings ON public.appointments;
CREATE TRIGGER trg_record_completed_scheduled_demo_earnings
AFTER UPDATE OF status ON public.appointments FOR EACH ROW
WHEN (NEW.status='completed' AND OLD.status IS DISTINCT FROM NEW.status)
EXECUTE FUNCTION public.record_completed_scheduled_demo_earnings();

-- Backfill verified historic services only. Do not invent releases for
-- paid appointments marked completed with missing participant/call evidence.
WITH eligible AS (
 SELECT DISTINCT ON (cr.id)
    cr.id request_id,p.id payment_id,cr.claimed_doctor_id doctor_id,
    cr.price_minor total_minor,s.commission_bps
 FROM public.consultation_requests cr
 JOIN public.payments p ON p.consultation_request_id=cr.id
 CROSS JOIN public.demo_platform_settings s
 WHERE cr.status='completed' AND cr.claimed_doctor_id IS NOT NULL
   AND p.status='paid' AND p.gateway_provider='ehealthy_demo'
   AND p.payment_method='demo_card'
   AND p.payment_type=CASE WHEN cr.mode='broadcast' THEN 'broadcast_consultation' ELSE 'private_consultation' END
   AND p.patient_id=cr.patient_id AND p.doctor_id=cr.claimed_doctor_id
   AND p.currency='ZAR' AND round(p.amount*100)::integer=cr.price_minor
 ORDER BY cr.id,p.created_at DESC
)
INSERT INTO public.demo_on_demand_earnings(
  consultation_request_id,payment_id,doctor_id,currency,total_minor,
  commission_bps,platform_minor,doctor_minor)
SELECT request_id,payment_id,doctor_id,'ZAR',total_minor,
  commission_bps,round(total_minor::numeric*commission_bps/10000)::integer,
  total_minor-round(total_minor::numeric*commission_bps/10000)::integer
FROM eligible ON CONFLICT DO NOTHING;

WITH eligible AS (
 SELECT DISTINCT ON (a.id)
  a.id appointment_id,p.id payment_id,a.doctor_id,
  a.price_minor total_minor,s.commission_bps
 FROM public.appointments a
 JOIN public.payments p ON p.appointment_id=a.id
 CROSS JOIN public.demo_platform_settings s
 WHERE a.status='completed' AND a.appointment_type='online'
   AND a.call_joined_patient_at IS NOT NULL
   AND a.call_joined_doctor_at IS NOT NULL AND a.call_ended_at IS NOT NULL
   AND p.status='paid' AND p.gateway_provider='ehealthy_demo'
   AND p.payment_type='scheduled_online' AND p.currency='ZAR'
   AND p.doctor_id=a.doctor_id AND p.patient_id=a.patient_id
   AND round(p.amount*100)::integer=a.price_minor
 ORDER BY a.id,p.created_at DESC
)
INSERT INTO public.demo_earnings(
 appointment_id,payment_id,doctor_id,currency,total_minor,
 commission_bps,platform_minor,doctor_minor)
SELECT appointment_id,payment_id,doctor_id,'ZAR',total_minor,commission_bps,
 round(total_minor::numeric*commission_bps/10000)::integer,
 total_minor-round(total_minor::numeric*commission_bps/10000)::integer
FROM eligible ON CONFLICT DO NOTHING;
