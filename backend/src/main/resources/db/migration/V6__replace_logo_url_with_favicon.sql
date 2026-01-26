-- Replace logo_url with favicon binary storage
-- Drop the logo_url column
ALTER TABLE services DROP COLUMN IF EXISTS logo_url;

-- Add favicon columns
ALTER TABLE services ADD COLUMN favicon BYTEA;
ALTER TABLE services ADD COLUMN favicon_content_type VARCHAR(50);

-- Add constraint to limit favicon size (32KB = 32768 bytes)
ALTER TABLE services ADD CONSTRAINT favicon_size_limit CHECK (octet_length(favicon) <= 32768);
