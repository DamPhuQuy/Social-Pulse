-- Remove the old role column that was replaced by the roles many-to-many relationship in V10
ALTER TABLE users DROP COLUMN IF EXISTS role;
