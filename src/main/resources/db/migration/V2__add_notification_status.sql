-- 1. 새로운 Enum을 받을 컬럼 추가
ALTER TABLE users ADD COLUMN notification_status VARCHAR(50);

-- 2. 명시적 동의 값만 YES로 처리하고 나머지는 NO 처리 (안전한 기본값)
UPDATE users
SET notification_status =
        CASE
            WHEN notification_setting = 'MIN_10' THEN 'YES'
            WHEN notification_setting = 'MIN_30' THEN 'YES'
            WHEN notification_setting = 'HOUR_1' THEN 'YES'
            ELSE 'NO'
            END;

-- 3. 옛날 컬럼 삭제
ALTER TABLE users DROP COLUMN notification_setting;
