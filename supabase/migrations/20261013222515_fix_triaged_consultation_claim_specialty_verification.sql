-- Accept specialty-matched consultations with a fresh availability heartbeat.
-- Keep authorization, eligibility recheck, row locking, occupancy locking,
-- first-accept-wins, and payment-dependent video-session triggers.

CREATE OR REPLACE FUNCTION public.claim_consultation_request(p_request_id uuid, p_idempotency_key uuid)
 RETURNS TABLE(request_id uuid, claim_result text, claimed_doctor_id uuid, request_version integer, stream_call_id text)
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'pg_catalog', 'public'
AS $function$
DECLARE
    v_auth_user_id uuid;
    v_doctor_id uuid;

    v_request public.consultation_requests%ROWTYPE;
    v_recipient public.consultation_recipients%ROWTYPE;

    v_call_id text;
    v_new_version integer;
BEGIN

    -- =====================================================
    -- AUTHENTICATION
    -- =====================================================

    v_auth_user_id := auth.uid();

    IF v_auth_user_id IS NULL THEN
        RAISE EXCEPTION 'Authentication required';
    END IF;


    IF p_request_id IS NULL THEN
        RAISE EXCEPTION 'Request ID is required';
    END IF;


    IF p_idempotency_key IS NULL THEN
        RAISE EXCEPTION
            'An idempotency key is required';
    END IF;


    -- =====================================================
    -- RESOLVE DOCTOR PROFILE
    -- =====================================================

    SELECT d.id
    INTO v_doctor_id
    FROM public.doctors d
    WHERE d.user_id = v_auth_user_id;


    IF v_doctor_id IS NULL THEN
        RAISE EXCEPTION 'Doctor profile not found';
    END IF;


    -- Prevent reusing the same idempotency key on another request.
    IF EXISTS (
        SELECT 1
        FROM public.consultation_recipients cr
        WHERE cr.doctor_id = v_doctor_id
          AND cr.claim_idempotency_key = p_idempotency_key
          AND cr.request_id <> p_request_id
    )
    THEN
        RAISE EXCEPTION
            'Idempotency key already used for another consultation';
    END IF;


    -- =====================================================
    -- LOCK REQUEST
    --
    -- This is the critical race-control point.
    -- =====================================================

    SELECT *
    INTO v_request
    FROM public.consultation_requests
    WHERE id = p_request_id
    FOR UPDATE;


    IF NOT FOUND THEN
        RAISE EXCEPTION
            'Consultation request not found';
    END IF;


    -- =====================================================
    -- ALREADY CLAIMED
    -- =====================================================

    IF v_request.status = 'claimed' THEN

        IF v_request.claimed_doctor_id = v_doctor_id THEN

            SELECT cs.stream_call_id
            INTO v_call_id
            FROM public.consultation_sessions cs
            WHERE cs.request_id = p_request_id;


            RETURN QUERY
            SELECT
                p_request_id,
                'claimed'::text,
                v_doctor_id,
                v_request.version,
                v_call_id;

            RETURN;

        ELSE

            RETURN QUERY
            SELECT
                p_request_id,
                'already_claimed'::text,
                NULL::uuid,
                v_request.version,
                NULL::text;

            RETURN;

        END IF;

    END IF;


    -- Terminal states cannot be claimed.
    IF v_request.status <> 'pending' THEN

        RETURN QUERY
        SELECT
            p_request_id,
            v_request.status,
            NULL::uuid,
            v_request.version,
            NULL::text;

        RETURN;

    END IF;


    -- =====================================================
    -- EXPIRED REQUEST
    -- =====================================================

    IF v_request.expires_at <= now() THEN

        v_new_version := v_request.version + 1;


        UPDATE public.consultation_requests
        SET
            status = 'expired',
            version = v_new_version
        WHERE id = p_request_id;


        UPDATE public.consultation_recipients
        SET status = 'closed'
        WHERE public.consultation_recipients.request_id = p_request_id
          AND status = 'pending';


        INSERT INTO public.notification_outbox (
            request_id,
            doctor_id,
            event_type,
            request_version,
            payload
        )
        SELECT
            p_request_id,
            cr.doctor_id,
            'consultation_closed',
            v_new_version,
            jsonb_build_object(
                'type', 'consultation_closed',
                'request_id', p_request_id,
                'version', v_new_version
            )
        FROM public.consultation_recipients cr
        WHERE cr.request_id = p_request_id

        ON CONFLICT DO NOTHING;


        RETURN QUERY
        SELECT
            p_request_id,
            'expired'::text,
            NULL::uuid,
            v_new_version,
            NULL::text;

        RETURN;

    END IF;


    -- =====================================================
    -- DOCTOR MUST BE A RECIPIENT
    -- =====================================================

    SELECT *
    INTO v_recipient
    FROM public.consultation_recipients
    WHERE public.consultation_recipients.request_id = p_request_id
      AND doctor_id = v_doctor_id
    FOR UPDATE;


    IF NOT FOUND THEN
        RAISE EXCEPTION
            'Doctor is not eligible for this consultation';
    END IF;


    IF v_recipient.status <> 'pending' THEN
        RAISE EXCEPTION
            'Consultation invitation is no longer available';
    END IF;


    -- =====================================================
    -- RECHECK ELIGIBILITY AT CLAIM TIME
    --
    -- A doctor may have gone offline after receiving FCM.
    -- =====================================================

    IF NOT EXISTS (
        SELECT 1
        FROM public.doctors d

        JOIN public.doctor_presence dp
          ON dp.doctor_id = d.id

        WHERE d.id = v_doctor_id

          AND d.verification_status = 'approved'

          AND COALESCE(
              d.is_deactivated,
              false
          ) = false

          AND dp.is_available = true

          AND dp.last_heartbeat >
              now() - interval '2 minutes'

          -- Presence scope is still general; medical specialty is stored in
          -- consultation_requests.service and checked against the doctor's
          -- verified discipline for broadcast requests.
          AND 'general' = ANY(dp.service_scope)
          AND (
             v_request.mode = 'private'
             OR (
               v_request.mode = 'broadcast'
               AND (
                 (v_request.service = 'general' AND d.discipline ~* '(general|family|primary[[:space:]]*care|medical[[:space:]]*officer|nurse)')
                 OR (v_request.service = 'dental' AND d.discipline ~* '(dentist|dental|orthodont)')
                 OR (v_request.service = 'heart' AND d.discipline ~* '(cardiolog|cardiac|heart)')
                 OR (v_request.service = 'mental_health' AND d.discipline ~* '(psycholog|psychiatr|mental[[:space:]]*health)')
                 OR (v_request.service = 'womens_health' AND d.discipline ~* '(gyn(ae|e)colog|obstetric|women)')
                 OR (v_request.service = 'children' AND d.discipline ~* '(paediatric|pediatric|children|child[[:space:]]*health)')
                 OR (v_request.service = 'skin' AND d.discipline ~* '(dermatolog|skin)')
                 OR (v_request.service = 'eyes' AND d.discipline ~* '(ophthalmolog|optometr|eye[[:space:]]*care)')
               )
             )
          )
    )
    THEN
        RAISE EXCEPTION
            'Doctor is no longer available';
    END IF;


    -- =====================================================
    -- REMOVE EXPIRED OCCUPANCY LOCK FOR THIS DOCTOR
    -- =====================================================

    DELETE FROM public.doctor_consultation_locks
    WHERE doctor_id = v_doctor_id
      AND lease_expires_at <= now();


    -- =====================================================
    -- OCCUPANCY LOCK
    --
    -- Prevent the same doctor accepting two different
    -- patients at the same time.
    --
    -- 2 hours is a technical safety lease, NOT the
    -- consultation duration.
    -- =====================================================

    BEGIN

        INSERT INTO public.doctor_consultation_locks (
            doctor_id,
            request_id,
            acquired_at,
            lease_expires_at
        )
        VALUES (
            v_doctor_id,
            p_request_id,
            now(),
            now() + interval '2 hours'
        );

    EXCEPTION
        WHEN unique_violation THEN

            RAISE EXCEPTION
                'Doctor is already occupied with another consultation';

    END;


    -- =====================================================
    -- THIS DOCTOR WINS
    -- =====================================================

    v_new_version := v_request.version + 1;


    UPDATE public.consultation_requests
    SET
        status = 'claimed',
        claimed_doctor_id = v_doctor_id,
        claimed_at = now(),
        version = v_new_version
    WHERE id = p_request_id;


    UPDATE public.consultation_recipients
    SET
        status = 'claimed',
        responded_at = now(),
        claim_idempotency_key = p_idempotency_key
    WHERE public.consultation_recipients.request_id = p_request_id
      AND doctor_id = v_doctor_id;


    -- Everybody else stops ringing.
    UPDATE public.consultation_recipients
    SET
        status = 'closed',
        responded_at = now()
    WHERE public.consultation_recipients.request_id = p_request_id
      AND doctor_id <> v_doctor_id
      AND status = 'pending';


    -- =====================================================
    -- CREATE A PENDING PRIVATE STREAM SESSION
    --
    -- Actual Stream provisioning still happens in trusted
    -- backend code.
    -- =====================================================

    v_call_id :=
        'consultation-' || p_request_id::text;


    INSERT INTO public.consultation_sessions (
        request_id,
        stream_call_id,
        status
    )
    VALUES (
        p_request_id,
        v_call_id,
        'pending'
    )

    ON CONFLICT ON CONSTRAINT consultation_sessions_request_id_key
    DO NOTHING;


    -- =====================================================
    -- STOP-RING NOTIFICATIONS FOR LOSING DOCTORS
    -- =====================================================

    INSERT INTO public.notification_outbox (
        request_id,
        doctor_id,
        event_type,
        request_version,
        payload
    )
    SELECT
        p_request_id,
        cr.doctor_id,
        'consultation_closed',
        v_new_version,
        jsonb_build_object(
            'type', 'consultation_closed',
            'request_id', p_request_id,
            'version', v_new_version
        )
    FROM public.consultation_recipients cr
    WHERE cr.request_id = p_request_id
      AND cr.doctor_id <> v_doctor_id

    ON CONFLICT DO NOTHING;


    RETURN QUERY
    SELECT
        p_request_id,
        'claimed'::text,
        v_doctor_id,
        v_new_version,
        v_call_id;

END;
$function$;

REVOKE ALL ON FUNCTION public.claim_consultation_request(uuid,uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.claim_consultation_request(uuid,uuid) TO authenticated;
