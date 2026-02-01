-- Create payment_status enum type
CREATE TYPE payment_status AS ENUM ('pending', 'paid', 'skipped');

-- Add status and paid_date columns to payment_records table
ALTER TABLE payment_records
    ADD COLUMN status payment_status NOT NULL DEFAULT 'pending',
    ADD COLUMN paid_date DATE;

-- Update existing records to be marked as paid (since they were actual payments)
UPDATE payment_records SET status = 'paid', paid_date = payment_date;
