-- Align the Android reviews.patient_id (public.patients.id) with auth.users.id.
-- Applied to the linked Supabase project as migration 20261015030644.
-- Only a signed-in patient may review their own completed appointment and doctor.
DROP POLICY IF EXISTS "Patients can insert own reviews" ON public.reviews;
CREATE POLICY "Patients can insert own reviews" ON public.reviews
FOR INSERT TO authenticated
WITH CHECK (
  EXISTS (
    SELECT 1
    FROM public.patients p
    JOIN public.appointments a ON a.patient_id = p.id
    WHERE p.id = reviews.patient_id
      AND p.user_id = (SELECT auth.uid())
      AND a.id = reviews.appointment_id
      AND a.doctor_id = reviews.doctor_id
      AND lower(a.status) = 'completed'
  )
);

DROP POLICY IF EXISTS "Patients can read own reviews" ON public.reviews;
CREATE POLICY "Patients can read own reviews" ON public.reviews
FOR SELECT TO authenticated
USING (
  EXISTS (
    SELECT 1 FROM public.patients p
    WHERE p.id = reviews.patient_id AND p.user_id = (SELECT auth.uid())
  )
);

CREATE UNIQUE INDEX IF NOT EXISTS reviews_one_per_appointment_idx
ON public.reviews(patient_id, appointment_id)
WHERE appointment_id IS NOT NULL;
