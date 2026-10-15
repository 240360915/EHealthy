-- Trigger helpers are internal and must not be directly callable from the Data API.
REVOKE ALL ON FUNCTION public.record_completed_on_demand_demo_earnings() FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.record_completed_scheduled_demo_earnings() FROM PUBLIC, anon, authenticated;
