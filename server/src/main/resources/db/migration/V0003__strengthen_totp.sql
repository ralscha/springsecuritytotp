ALTER TABLE app_user ALTER COLUMN secret VARCHAR(64);
ALTER TABLE app_user ADD COLUMN last_totp_interval BIGINT;
