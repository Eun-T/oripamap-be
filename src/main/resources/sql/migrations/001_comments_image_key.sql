-- 기존 DB에 한 번만 실행한다. 신규 DB는 init/002_tables.sql에 이미 포함되어 있다.
ALTER TABLE comments
    ADD COLUMN image_key VARCHAR(500) NULL;
