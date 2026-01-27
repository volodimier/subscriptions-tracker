-- Add trigger_type column to track whether job was scheduled or manually triggered
CREATE TYPE trigger_type AS ENUM ('SCHEDULED', 'MANUAL');

ALTER TABLE job_runs ADD COLUMN trigger_type trigger_type NOT NULL DEFAULT 'SCHEDULED';
