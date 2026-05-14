-- Add edited column to comments table
ALTER TABLE comments ADD COLUMN edited BOOLEAN NOT NULL DEFAULT FALSE;
