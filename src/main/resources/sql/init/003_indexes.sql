-- ============================================================
-- PLACES
-- ============================================================

-- 오리파 / 포켓몬 자판기 필터
CREATE INDEX idx_places_type
    ON places(type);

-- 이름 검색
CREATE INDEX idx_places_name
    ON places(name);

-- 지도 영역 조회
CREATE INDEX idx_places_location
    ON places(latitude, longitude);


-- ============================================================
-- PLACE_IMAGES
-- ============================================================

-- 장소 상세 → 이미지 조회
CREATE INDEX idx_place_images_place
    ON place_images(place_id);


-- ============================================================
-- COMMENTS
-- ============================================================

-- 장소 상세 → 댓글 조회
CREATE INDEX idx_comments_place
    ON comments(place_id);

-- 유저가 작성한 댓글 조회
CREATE INDEX idx_comments_user
    ON comments(user_id);


-- ============================================================
-- FAVORITES
-- ============================================================

-- 장소의 저장 수/저장 사용자 조회
CREATE INDEX idx_favorites_place
    ON favorites(place_id);