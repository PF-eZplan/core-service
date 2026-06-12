-- 1. 새로운 Enum을 받을 컬럼을 먼저 추가
ALTER TABLE users ADD COLUMN notification_status VARCHAR(50);

-- 2. 데이터 마이그레이션 (기존 데이터 보존)
UPDATE users
SET notification_status =
        CASE
            WHEN notification_setting = 'NONE' THEN 'NO'
            WHEN notification_setting IS NULL THEN 'NO'
            ELSE 'YES'
            END;

-- 3. 데이터 이관이 완료된 후 옛날 컬럼 삭제
ALTER TABLE users DROP COLUMN notification_setting;
