USE oripa;

-- Add as nullable first so existing EVENT images can be backfilled safely.
ALTER TABLE event_place_images
    ADD COLUMN image_type ENUM('COVER', 'CONTENT') NULL AFTER image_key;

UPDATE event_place_images
SET image_type = 'COVER'
WHERE image_type IS NULL;

ALTER TABLE event_place_images
    MODIFY COLUMN image_type ENUM('COVER', 'CONTENT') NOT NULL;
