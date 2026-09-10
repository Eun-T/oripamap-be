-- Run this query first. The ALTER below will fail while this returns any rows.
SELECT nickname, COUNT(*) AS duplicate_count
FROM users
GROUP BY nickname
HAVING COUNT(*) > 1;

ALTER TABLE users
    ADD CONSTRAINT uk_users_nickname UNIQUE (nickname);
