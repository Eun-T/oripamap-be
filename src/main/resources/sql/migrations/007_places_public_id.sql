-- MySQL 8.0.13+
-- Existing rows are populated before NOT NULL and UNIQUE are applied.
ALTER TABLE places
    ADD COLUMN public_id CHAR(36) NULL AFTER id;

UPDATE places
SET public_id = UUID()
WHERE public_id IS NULL;

ALTER TABLE places
    MODIFY COLUMN public_id CHAR(36) NOT NULL DEFAULT (UUID()),
    ADD CONSTRAINT uk_places_public_id UNIQUE (public_id);

-- Direct inserts may omit public_id because the column default generates it.
-- It can also be supplied explicitly: public_id = UUID().
