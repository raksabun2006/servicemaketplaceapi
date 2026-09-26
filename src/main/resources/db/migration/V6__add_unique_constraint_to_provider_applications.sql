-- Deduplicate existing applications keeping the most recently created one
DELETE FROM provider_applications a
USING provider_applications b
WHERE a.user_id = b.user_id
  AND a.created_at < b.created_at;

-- Add unique constraint on user_id to prevent duplicate provider applications per user
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_provider_applications_user_id'
    ) THEN
        ALTER TABLE provider_applications
            ADD CONSTRAINT uq_provider_applications_user_id UNIQUE (user_id);
    END IF;
END $$;
