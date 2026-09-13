ALTER TABLE inquiries
    ADD COLUMN answer TEXT NULL AFTER status,
    ADD COLUMN answered_at DATETIME NULL AFTER answer;
