ALTER TABLE app_user ADD COLUMN enrollment_pending BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE app_user ADD COLUMN enrollment_expires_at BIGINT;
