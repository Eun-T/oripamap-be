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

                       email VARCHAR(100),
                       password VARCHAR(255),
                       nickname VARCHAR(50) NOT NULL,

                       provider ENUM('LOCAL', 'KAKAO', 'NAVER')
        NOT NULL DEFAULT 'LOCAL',

                       provider_id VARCHAR(255),

                       profile_image_url VARCHAR(500),

                       created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

                       updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
                           ON UPDATE CURRENT_TIMESTAMP,

                       UNIQUE KEY uk_users_provider (
                           provider,
                           provider_id
                           )
);


-- ============================================================
-- 2. 장소
-- ORIPA / 포켓몬 카드 자판기
-- ============================================================
CREATE TABLE places (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,

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


-- ============================================================
-- 3. 장소 이미지
-- ============================================================
CREATE TABLE place_images (
                              id BIGINT AUTO_INCREMENT PRIMARY KEY,

                              place_id BIGINT NOT NULL,

                              user_id BIGINT,

                              image_url VARCHAR(500) NOT NULL,

                              sort_order INT DEFAULT 0,

                              created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

                              CONSTRAINT fk_place_images_place
                                  FOREIGN KEY (place_id)
                                      REFERENCES places(id)
                                      ON DELETE CASCADE,

                              CONSTRAINT fk_place_images_user
                                  FOREIGN KEY (user_id)
                                      REFERENCES users(id)
                                      ON DELETE SET NULL
);


-- ============================================================
-- 4. 댓글
-- ============================================================
CREATE TABLE comments (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,

                          place_id BIGINT NOT NULL,

                          user_id BIGINT NOT NULL,

                          content TEXT NOT NULL,

                          created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

                          updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
                              ON UPDATE CURRENT_TIMESTAMP,

                          CONSTRAINT fk_comments_place
                              FOREIGN KEY (place_id)
                                  REFERENCES places(id)
                                  ON DELETE CASCADE,

                          CONSTRAINT fk_comments_user
                              FOREIGN KEY (user_id)
                                  REFERENCES users(id)
                                  ON DELETE CASCADE
);


-- ============================================================
-- 5. 저장 / 즐겨찾기
-- ============================================================
CREATE TABLE favorites (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,

                           place_id BIGINT NOT NULL,

                           user_id BIGINT NOT NULL,

                           created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

                           CONSTRAINT fk_favorites_place
                               FOREIGN KEY (place_id)
                                   REFERENCES places(id)
                                   ON DELETE CASCADE,

                           CONSTRAINT fk_favorites_user
                               FOREIGN KEY (user_id)
                                   REFERENCES users(id)
                                   ON DELETE CASCADE,

                           CONSTRAINT uk_favorites_user_place
                               UNIQUE (user_id, place_id)
);

CREATE TABLE edit_requests (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,

                               place_id BIGINT NOT NULL,
                               user_id BIGINT NOT NULL,

                               request_types VARCHAR(500) NOT NULL,
                               memo TEXT,

                               status ENUM('PENDING', 'APPROVED', 'REJECTED')
        NOT NULL DEFAULT 'PENDING',

                               created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

                               CONSTRAINT fk_edit_requests_place
                                   FOREIGN KEY (place_id)
                                       REFERENCES places(id)
                                       ON DELETE CASCADE,

                               CONSTRAINT fk_edit_requests_user
                                   FOREIGN KEY (user_id)
                                       REFERENCES users(id)
                                       ON DELETE CASCADE,

                               INDEX idx_edit_requests_place_id (place_id),
                               INDEX idx_edit_requests_user_id (user_id),
                               INDEX idx_edit_requests_status (status)
);