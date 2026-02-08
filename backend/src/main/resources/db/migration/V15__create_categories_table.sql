-- Create categories table for managed category system
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Unique constraint: prevent duplicate names within scope (system or per-user)
CREATE UNIQUE INDEX uq_categories_name_scope ON categories (name, COALESCE(user_id, 0));

-- Index on user_id for filtering
CREATE INDEX idx_categories_user_id ON categories(user_id);

-- Seed 14 system categories (user_id = NULL)
INSERT INTO categories (name) VALUES
    ('Entertainment'),
    ('Media'),
    ('Music'),
    ('Streaming'),
    ('Fitness'),
    ('Health'),
    ('Software'),
    ('Storage'),
    ('News'),
    ('Education'),
    ('Gaming'),
    ('Productivity'),
    ('Business'),
    ('Other');

-- Add category_id column to services table
ALTER TABLE services ADD COLUMN category_id BIGINT REFERENCES categories(id);

-- Migrate existing data: case-insensitive match to system categories, fallback to "Other"
UPDATE services s
SET category_id = COALESCE(
    (SELECT c.id FROM categories c WHERE c.user_id IS NULL AND LOWER(c.name) = LOWER(s.category)),
    (SELECT c.id FROM categories c WHERE c.user_id IS NULL AND c.name = 'Other')
)
WHERE s.category IS NOT NULL;

-- Drop old category column
ALTER TABLE services DROP COLUMN category;

-- Drop old index on category (created in V2)
DROP INDEX IF EXISTS idx_services_category;

-- Index on services.category_id
CREATE INDEX idx_services_category_id ON services(category_id);
