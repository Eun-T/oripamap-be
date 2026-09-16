USE oripa;

ALTER TABLE places
    MODIFY COLUMN type ENUM('ORIPA', 'POKEMON_VENDING', 'EVENT') NOT NULL;

CREATE TABLE event_place
(
    place_id       BIGINT NOT NULL PRIMARY KEY,
    start_date     DATE NULL,
    end_date       DATE NULL,
    event_hours    VARCHAR(255) NULL,
    benefits       TEXT NULL,
    notice         TEXT NULL,
    summary        VARCHAR(255) NULL,
    introduction  TEXT NULL,
    social_links   JSON NULL,
    created_at     DATETIME DEFAULT CURRENT_TIMESTAMP NULL,
    updated_at     DATETIME DEFAULT CURRENT_TIMESTAMP NULL
                   ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_event_place
        FOREIGN KEY (place_id) REFERENCES places (id) ON DELETE CASCADE
);

CREATE TABLE event_place_images
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    place_id   BIGINT NOT NULL,
    image_key  VARCHAR(500) NOT NULL,
    sort_order INT DEFAULT 0 NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NULL,
    CONSTRAINT fk_event_place_images
        FOREIGN KEY (place_id) REFERENCES event_place (place_id) ON DELETE CASCADE
);
