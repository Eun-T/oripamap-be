USE oripa;

-- Add as nullable first, then explicitly backfill existing EVENT rows.
ALTER TABLE event_place
    ADD COLUMN event_type ENUM('OFFLINE', 'ONLINE') NULL DEFAULT 'OFFLINE' AFTER place_id;

UPDATE event_place
SET event_type = 'OFFLINE'
WHERE event_type IS NULL;

ALTER TABLE event_place
    MODIFY COLUMN event_type ENUM('OFFLINE', 'ONLINE') NOT NULL DEFAULT 'OFFLINE';

ALTER TABLE places
    MODIFY COLUMN address VARCHAR(255) NULL,
    MODIFY COLUMN latitude DECIMAL(10, 7) NULL,
    MODIFY COLUMN longitude DECIMAL(10, 7) NULL;
