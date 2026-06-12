-- 기존 컬럼(일괄 알림 설정) 삭제
ALTER TABLE users DROP COLUMN IF EXISTS notification_setting;

-- 새로운 Enum을 받을 컬럼(알림수신여부) 추가
ALTER TABLE users ADD COLUMN notification_status VARCHAR(50);
