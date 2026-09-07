-- Run this once against an existing database.
-- The ALTER intentionally fails when duplicate or NULL emails already exist.
-- Inspect and resolve them first with:
-- SELECT email, COUNT(*) FROM users GROUP BY email HAVING COUNT(*) > 1;
-- SELECT id FROM users WHERE email IS NULL;
ALTER TABLE users
    MODIFY email VARCHAR(100) NOT NULL,
    ADD CONSTRAINT uk_users_email UNIQUE (email);
