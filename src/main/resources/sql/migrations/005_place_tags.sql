CREATE TABLE tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE,
    category VARCHAR(30) NULL
);

CREATE TABLE place_tags (
    place_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (place_id, tag_id),

    CONSTRAINT fk_place_tags_place
        FOREIGN KEY (place_id)
        REFERENCES places(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_place_tags_tag
        FOREIGN KEY (tag_id)
        REFERENCES tags(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_place_tags_tag_id
    ON place_tags(tag_id);
