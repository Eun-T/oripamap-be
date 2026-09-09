ALTER TABLE users
    ADD COLUMN role ENUM('USER', 'OWNER', 'ADMIN') NOT NULL DEFAULT 'USER'
        AFTER provider_id,
    ADD COLUMN place_id BIGINT NULL AFTER role,
    ADD CONSTRAINT fk_users_place
        FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE SET NULL,
    ADD CONSTRAINT chk_users_place_role
        CHECK (role = 'OWNER' OR place_id IS NULL),
    ADD INDEX idx_users_place (place_id);
