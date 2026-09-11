USE oripa;

-- ============================================================
-- FILE : 002_tables.sql
-- Description : Oripa Map 테이블 생성
-- ============================================================


-- 기존 테이블 제거
-- DROP TABLE IF EXISTS favorites;
-- DROP TABLE IF EXISTS comments;
-- DROP TABLE IF EXISTS place_images;
-- DROP TABLE IF EXISTS places;
-- DROP TABLE IF EXISTS users;


-- ============================================================
-- 1. 사용자
-- ============================================================
CREATE TABLE users (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,

                       email VARCHAR(100) NOT NULL,
                       password VARCHAR(255),
                       nickname VARCHAR(50) NOT NULL,

                       provider ENUM('LOCAL', 'KAKAO', 'NAVER')
        NOT NULL DEFAULT 'LOCAL',

                       provider_id VARCHAR(255),

                       role ENUM('USER', 'OWNER', 'ADMIN')
                           NOT NULL DEFAULT 'USER',

                       place_id BIGINT NULL,


                       created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

                       updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
                           ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE refresh_tokens (
                                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                user_id BIGINT NOT NULL,
                                token_hash CHAR(64) NOT NULL,
                                expires_at DATETIME NOT NULL,
                                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                revoked_at DATETIME NULL
);


-- ============================================================
-- 2. 장소
-- ORIPA / 포켓몬 카드 자판기
-- ============================================================
CREATE TABLE places (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        public_id CHAR(36) NOT NULL DEFAULT (UUID()),

                        type ENUM(
        'ORIPA',
        'POKEMON_VENDING'
    ) NOT NULL,

                        name VARCHAR(100) NOT NULL,

                        branch_name VARCHAR(100),

                        address VARCHAR(255) NOT NULL,

                        location_detail VARCHAR(255),

                        latitude DECIMAL(10, 7) NOT NULL,

                        longitude DECIMAL(10, 7) NOT NULL,

                        business_hours VARCHAR(255),

                        holiday_info VARCHAR(255),

                        phone VARCHAR(30),

                        description TEXT,

                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

                        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
                            ON UPDATE CURRENT_TIMESTAMP
);

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


-- ============================================================
-- 3. 장소 이미지
-- ============================================================
CREATE TABLE place_images (
                              id BIGINT AUTO_INCREMENT PRIMARY KEY,

                              place_id BIGINT NOT NULL,

                              user_id BIGINT,

                              image_url VARCHAR(500) NOT NULL,

                              sort_order INT DEFAULT 0,

                              created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================
-- 4. 댓글
-- ============================================================
CREATE TABLE comments (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,

                          place_id BIGINT NOT NULL,

                          user_id BIGINT NOT NULL,

                          parent_comment_id BIGINT NULL,

                          content TEXT NOT NULL,

                          image_key VARCHAR(500) NULL,

                          created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

                          updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
                              ON UPDATE CURRENT_TIMESTAMP
);


-- ============================================================
-- 5. 저장 / 즐겨찾기
-- ============================================================
CREATE TABLE favorites (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,

                           place_id BIGINT NOT NULL,

                           user_id BIGINT NOT NULL,

                           created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE edit_requests (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,

                               place_id BIGINT NOT NULL,
                               user_id BIGINT NOT NULL,

                               request_types VARCHAR(500) NOT NULL,
                               memo TEXT,

                               status ENUM('PENDING', 'APPROVED', 'REJECTED')
        NOT NULL DEFAULT 'PENDING',

                               created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE oripa_place (
                             place_id BIGINT PRIMARY KEY,
                             summary VARCHAR(255) NULL,
                             introduction TEXT NULL,
                             social_links JSON NULL,

                             created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                             updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
                                 ON UPDATE CURRENT_TIMESTAMP,

                             CONSTRAINT fk_oripa_place
                                 FOREIGN KEY (place_id)
                                     REFERENCES places(id)
                                     ON DELETE CASCADE
);

CREATE TABLE oripa_place_images (
                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    place_id BIGINT NOT NULL,
                                    image_key VARCHAR(500) NOT NULL,
                                    sort_order INT NOT NULL DEFAULT 0,

                                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

                                    CONSTRAINT fk_oripa_place_images
                                        FOREIGN KEY (place_id)
                                            REFERENCES oripa_place(place_id)
                                            ON DELETE CASCADE
);
