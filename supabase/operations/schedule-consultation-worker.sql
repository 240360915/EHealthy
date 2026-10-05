-- Review and execute only after deployment and secure Vault configuration.
-- Does not set a tariff, change RLS/schema, or alter appointment-cron-job.
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM vault.secrets WHERE name = 'consultation_worker_secret') THEN
    RAISE EXCEPTION 'Configure consultation_worker_secret in Vault first';
  END IF;
END $$;

SELECT cron.schedule(
  'consultation-notification-worker',
  '10 seconds',
  $job$
  SELECT net.http_post(
    url := 'https://gqyhkccupbeudenvsdsf.supabase.co/functions/v1/consultation-worker',
    headers := jsonb_build_object(
      'Content-Type', 'application/json',
      'x-consultation-worker-secret',
      (SELECT decrypted_secret FROM vault.decrypted_secrets WHERE name = 'consultation_worker_secret')
    ),
    body := '{}'::jsonb,
    timeout_milliseconds := 120000
  );
  $job$
);
