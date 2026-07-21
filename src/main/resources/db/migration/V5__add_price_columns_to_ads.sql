-- Brings databases created from the original V2 up to date: the current V2
-- already contains these columns, so on fresh installs this migration is a no-op.
ALTER TABLE ads
    ADD COLUMN IF NOT EXISTS price_is_numeric BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE ads
    ALTER COLUMN price_is_numeric DROP DEFAULT;

ALTER TABLE ads
    ADD COLUMN IF NOT EXISTS price_text VARCHAR(255);
