-- EHealthy: specialty-aware on-demand consultation broadcast.
-- Patient notes are visible only via authenticated participant/doctor paths.
-- Legacy RPCs remain available for older Android builds.
ALTER TABLE public.consultation_requests
    ADD COLUMN IF NOT EXISTS symptom_notes text,
    ADD COLUMN IF NOT EXISTS urgency text NOT NULL DEFAULT 'routine';

ALTER TABLE public.consultation_requests
    DROP CONSTRAINT IF EXISTS consultation_symptom_notes_length;
ALTER TABLE public.consultation_requests
    ADD CONSTRAINT consultation_symptom_notes_length
    CHECK (symptom_notes IS NULL OR char_length(symptom_notes) <= 800);

ALTER TABLE public.consultation_requests
    DROP CONSTRAINT IF EXISTS consultation_urgency_allowed;
ALTER TABLE public.consultation_requests
    ADD CONSTRAINT consultation_urgency_allowed
    CHECK (urgency IN ('routine','soon','urgent'));

CREATE OR REPLACE FUNCTION public.create_triaged_consultation_request(p_mode text, p_target_doctor_id uuid, p_service text, p_idempotency_key uuid, p_symptom_notes text, p_urgency text)
 RETURNS TABLE(request_id uuid, request_status text, request_mode text, recipient_count integer, amount_minor integer, payment_currency text, expires_at timestamp with time zone, request_version integer)
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'pg_catalog', 'public'
AS $function$
DECLARE
    v_auth_user_id uuid;
    v_patient_id uuid;

    v_mode text;
    v_service text;
    v_symptom_notes text;
    v_urgency text;

    v_price_minor integer;
    v_currency text := 'ZAR';

    v_request_id uuid;
    v_expires_at timestamptz;
    v_recipient_count integer := 0;

    v_existing public.consultation_requests%ROWTYPE;
BEGIN

    v_auth_user_id := auth.uid();

    IF v_auth_user_id IS NULL THEN
        RAISE EXCEPTION 'Authentication required';
    END IF;

    IF p_idempotency_key IS NULL THEN
        RAISE EXCEPTION 'An idempotency key is required';
    END IF;


    v_mode :=
        lower(btrim(COALESCE(p_mode, '')));

    v_service :=
        lower(btrim(COALESCE(p_service, 'general')));


    IF v_service = '' THEN v_service := 'general'; END IF;
    IF v_service NOT IN ('general','dental','heart','mental_health','womens_health','children','skin','eyes') THEN
        RAISE EXCEPTION 'Select a valid consultation specialty';
    END IF;
    v_symptom_notes := btrim(coalesce(p_symptom_notes,''));
    IF char_length(v_symptom_notes) < 10 OR char_length(v_symptom_notes) > 800 THEN
        RAISE EXCEPTION 'Write between 10 and 800 characters describing the reason for consultation';
    END IF;
    v_urgency := lower(btrim(coalesce(p_urgency,'routine')));
    IF v_urgency NOT IN ('routine','soon','urgent') THEN RAISE EXCEPTION 'Select a valid urgency'; END IF;


    IF v_mode NOT IN ('private', 'broadcast') THEN
        RAISE EXCEPTION
            'Consultation mode must be private or broadcast';
    END IF;


    IF v_mode = 'private'
       AND p_target_doctor_id IS NULL
    THEN
        RAISE EXCEPTION
            'Private consultation requires a doctor';
    END IF;


    IF v_mode = 'broadcast'
       AND p_target_doctor_id IS NOT NULL
    THEN
        RAISE EXCEPTION
            'Broadcast consultation cannot specify a doctor';
    END IF;


    -- =====================================================
    -- AUTH USER -> PATIENT PROFILE
    -- =====================================================

    SELECT p.id
    INTO v_patient_id
    FROM public.patients p
    WHERE p.user_id = v_auth_user_id;


    IF v_patient_id IS NULL THEN
        RAISE EXCEPTION 'Patient profile not found';
    END IF;


    -- =====================================================
    -- IDEMPOTENCY
    -- =====================================================

    SELECT *
    INTO v_existing
    FROM public.consultation_requests r
    WHERE r.patient_id = v_patient_id
      AND r.idempotency_key = p_idempotency_key;


    IF FOUND THEN

        IF v_existing.mode <> v_mode
           OR v_existing.service <> v_service
           OR v_existing.symptom_notes IS DISTINCT FROM v_symptom_notes
           OR v_existing.urgency IS DISTINCT FROM v_urgency
           OR v_existing.target_doctor_id
                IS DISTINCT FROM p_target_doctor_id
        THEN
            RAISE EXCEPTION
                'Idempotency key already used for another request';
        END IF;


        SELECT COUNT(*)::integer
        INTO v_recipient_count
        FROM public.consultation_recipients cr
        WHERE cr.request_id = v_existing.id;


        RETURN QUERY
        SELECT
            v_existing.id,
            v_existing.status,
            v_existing.mode,
            v_recipient_count,
            v_existing.price_minor,
            v_existing.currency,
            v_existing.expires_at,
            v_existing.version;

        RETURN;
    END IF;


    IF EXISTS (
        SELECT 1
        FROM public.consultation_requests r
        WHERE r.patient_id = v_patient_id
          AND r.status IN ('pending', 'claimed')
    )
    THEN
        RAISE EXCEPTION
            'Patient already has an active consultation request';
    END IF;


    -- =====================================================
    -- PRICE
    --
    -- PRIVATE:
    -- selected doctor's own hourly_rate
    --
    -- BROADCAST:
    -- platform standard tariff
    -- =====================================================

    IF v_mode = 'private' THEN

        SELECT
            ROUND(d.hourly_rate * 100)::integer
        INTO v_price_minor
        FROM public.doctors d
        WHERE d.id = p_target_doctor_id
          AND d.verification_status = 'approved'
          AND COALESCE(d.is_deactivated, false) = false
          AND d.hourly_rate IS NOT NULL
          AND d.hourly_rate > 0;


        IF v_price_minor IS NULL THEN
            RAISE EXCEPTION
                'Selected doctor has no valid private consultation fee';
        END IF;


        v_currency := 'ZAR';

    ELSE

        SELECT
            cp.amount_minor,
            cp.currency
        INTO
            v_price_minor,
            v_currency
        FROM public.consultation_prices cp
        WHERE cp.service IN (v_service, 'general')
          AND cp.mode = 'broadcast'
          AND cp.enabled = true
          AND cp.effective_from <= now()
          AND (
              cp.effective_to IS NULL
              OR cp.effective_to > now()
          )
        ORDER BY (cp.service = v_service) DESC, cp.effective_from DESC
        LIMIT 1;


        IF v_price_minor IS NULL THEN
            RAISE EXCEPTION
                'No standard broadcast consultation price is configured';
        END IF;

    END IF;


    v_expires_at :=
        now() + interval '60 seconds';


    -- =====================================================
    -- CREATE REQUEST
    -- =====================================================

    INSERT INTO public.consultation_requests (
        patient_id,
        mode,
        service,
        symptom_notes,
        urgency,
        target_doctor_id,
        status,
        price_minor,
        currency,
        idempotency_key,
        version,
        expires_at
    )
    VALUES (
        v_patient_id,
        v_mode,
        v_service,
        v_symptom_notes,
        v_urgency,
        p_target_doctor_id,
        'pending',
        v_price_minor,
        v_currency,
        p_idempotency_key,
        1,
        v_expires_at
    )
    RETURNING id
    INTO v_request_id;


    -- =====================================================
    -- SELECT ELIGIBLE DOCTORS
    -- =====================================================

    IF v_mode = 'private' THEN

        INSERT INTO public.consultation_recipients (
            request_id,
            doctor_id,
            status
        )
        SELECT
            v_request_id,
            d.id,
            'pending'
        FROM public.doctors d

        JOIN public.doctor_presence dp
          ON dp.doctor_id = d.id

        WHERE d.id = p_target_doctor_id

          AND d.verification_status = 'approved'

          AND COALESCE(d.is_deactivated, false) = false

          AND dp.is_available = true

          AND dp.last_heartbeat >
              now() - interval '2 minutes'

          AND 'general' = ANY(dp.service_scope)

          AND NOT EXISTS (
              SELECT 1
              FROM public.doctor_consultation_locks l
              WHERE l.doctor_id = d.id
                AND l.lease_expires_at > now()
          );

    ELSE

        INSERT INTO public.consultation_recipients (
            request_id,
            doctor_id,
            status
        )
        SELECT
            v_request_id,
            d.id,
            'pending'
        FROM public.doctors d

        JOIN public.doctor_presence dp
          ON dp.doctor_id = d.id

        WHERE d.verification_status = 'approved'

          AND COALESCE(d.is_deactivated, false) = false

          AND dp.is_available = true

          AND dp.last_heartbeat >
              now() - interval '2 minutes'

          AND 'general' = ANY(dp.service_scope)

          AND (
            (v_service='general' AND d.discipline ~* '(general|family|primary[[:space:]]*care|medical[[:space:]]*officer|nurse)')
            OR (v_service='dental' AND d.discipline ~* '(dentist|dental|orthodont)')
            OR (v_service='heart' AND d.discipline ~* '(cardiolog|cardiac|heart)')
            OR (v_service='mental_health' AND d.discipline ~* '(psycholog|psychiatr|mental[[:space:]]*health)')
            OR (v_service='womens_health' AND d.discipline ~* '(gyn(ae|e)colog|obstetric|women)')
            OR (v_service='children' AND d.discipline ~* '(paediatric|pediatric|children|child[[:space:]]*health)')
            OR (v_service='skin' AND d.discipline ~* '(dermatolog|skin)')
            OR (v_service='eyes' AND d.discipline ~* '(ophthalmolog|optometr|eye[[:space:]]*care)')
          )
          AND NOT EXISTS (
              SELECT 1
              FROM public.doctor_consultation_locks l
              WHERE l.doctor_id = d.id
                AND l.lease_expires_at > now()
          );

    END IF;


    GET DIAGNOSTICS
        v_recipient_count = ROW_COUNT;


    -- =====================================================
    -- NOBODY AVAILABLE
    -- =====================================================

    IF v_recipient_count = 0 THEN

        UPDATE public.consultation_requests
        SET
            status = 'no_doctors',
            version = version + 1
        WHERE id = v_request_id;


        RETURN QUERY
        SELECT
            r.id,
            r.status,
            r.mode,
            0,
            r.price_minor,
            r.currency,
            r.expires_at,
            r.version
        FROM public.consultation_requests r
        WHERE r.id = v_request_id;

        RETURN;

    END IF;


    -- =====================================================
    -- QUEUE RING INVITATIONS
    -- =====================================================

    INSERT INTO public.notification_outbox (
        request_id,
        doctor_id,
        event_type,
        request_version,
        payload
    )
    SELECT
        v_request_id,
        cr.doctor_id,
        'consultation_invite',
        1,
        jsonb_build_object(
            'type', 'consultation_invite',
            'request_id', v_request_id,
            'version', 1,
            'expires_at', v_expires_at
        )
    FROM public.consultation_recipients cr
    WHERE cr.request_id = v_request_id;


    RETURN QUERY
    SELECT
        r.id,
        r.status,
        r.mode,
        v_recipient_count,
        r.price_minor,
        r.currency,
        r.expires_at,
        r.version
    FROM public.consultation_requests r
    WHERE r.id = v_request_id;

END;
$function$;

CREATE OR REPLACE FUNCTION public.doctor_get_triaged_consultation_requests()
 RETURNS TABLE(request_id uuid, patient_id uuid, patient_name text, request_mode text, service text, price_minor integer, currency text, request_status text, recipient_status text, created_at timestamp with time zone, expires_at timestamp with time zone, symptom_notes text, urgency text)
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
declare
    v_doctor_id uuid;
begin

    if auth.uid() is null then
        raise exception 'Authentication required';
    end if;

    select d.id
    into v_doctor_id
    from public.doctors d
    where d.user_id = auth.uid()
    limit 1;

    if v_doctor_id is null then
        raise exception 'Doctor profile not found';
    end if;

    return query
    select
        r.id as request_id,
        r.patient_id,
        trim(
            concat(
                coalesce(p.name, ''),
                ' ',
                coalesce(p.surname, '')
            )
        ) as patient_name,
        r.mode as request_mode,
        r.service,
        r.price_minor::integer,
        r.currency,
        r.status as request_status,
        cr.status as recipient_status,
        r.created_at,
        r.expires_at,
        r.symptom_notes,
        r.urgency
    from public.consultation_recipients cr
    join public.consultation_requests r
        on r.id = cr.request_id
    left join public.patients p
        on p.id = r.patient_id
    where cr.doctor_id = v_doctor_id
      and r.status = 'pending'
      and r.expires_at > now()
      and cr.status not in (
          'declined',
          'closed',
          'lost'
      )
    order by r.created_at asc;

end;
$function$;

REVOKE ALL ON FUNCTION public.create_triaged_consultation_request(text,uuid,text,uuid,text,text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.create_triaged_consultation_request(text,uuid,text,uuid,text,text) TO authenticated;
REVOKE ALL ON FUNCTION public.doctor_get_triaged_consultation_requests() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.doctor_get_triaged_consultation_requests() TO authenticated;
