ALTER TABLE bookings ALTER COLUMN scheduled_at DROP NOT NULL;
ALTER TABLE bookings ALTER COLUMN service_id DROP NOT NULL;
ALTER TABLE bookings DROP CONSTRAINT IF EXISTS bookings_status_check;
