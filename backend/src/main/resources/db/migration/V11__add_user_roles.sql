-- Add role column to users table for Role-Based Access Control (RBAC)
-- Default value is 'USER' for all existing and new users

ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';

-- Create index for efficient role-based queries
CREATE INDEX idx_users_role ON users(role);

-- Add check constraint to ensure valid role values
ALTER TABLE users ADD CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'));
