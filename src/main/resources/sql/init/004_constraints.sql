USE oripa;

-- ============================================================
-- UNIQUE CONSTRAINTS
-- ============================================================
ALTER TABLE users
    ADD CONSTRAINT uk_users_email
        UNIQUE (email),
    ADD CONSTRAINT uk_users_nickname
        UNIQUE (nickname),
    ADD CONSTRAINT uk_users_provider
        UNIQUE (provider, provider_id),
    ADD CONSTRAINT fk_users_place
        FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE SET NULL,
    ADD CONSTRAINT chk_users_place_role
        CHECK (role = 'OWNER' OR place_id IS NULL);

ALTER TABLE favorites
    ADD CONSTRAINT uk_favorites_user_place
        UNIQUE (user_id, place_id);

ALTER TABLE places
    ADD CONSTRAINT uk_places_public_id
        UNIQUE (public_id);

ALTER TABLE refresh_tokens
    ADD CONSTRAINT uk_refresh_tokens_hash
        UNIQUE (token_hash);


-- ============================================================
-- FOREIGN KEY CONSTRAINTS
-- ============================================================
ALTER TABLE refresh_tokens
    ADD CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE place_images
    ADD CONSTRAINT fk_place_images_place
        FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_place_images_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE comments
    ADD CONSTRAINT fk_comments_place
        FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_comments_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_comments_parent
        FOREIGN KEY (parent_comment_id) REFERENCES comments(id) ON DELETE CASCADE;

ALTER TABLE favorites
    ADD CONSTRAINT fk_favorites_place
        FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_favorites_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE edit_requests
    ADD CONSTRAINT fk_edit_requests_place
        FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_edit_requests_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
