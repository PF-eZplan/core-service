-- 1. users 테이블 (온보딩 필드 추가)
CREATE TABLE users
(
    id                   UUID PRIMARY KEY,
    email                VARCHAR(255) UNIQUE NOT NULL,
    password             VARCHAR(255),
    provider             VARCHAR(50)         NOT NULL,
    name                 VARCHAR(100)        NOT NULL,
    nickname             VARCHAR(100) UNIQUE,
    gender               VARCHAR(50),
    age_group            VARCHAR(50),
    job                  VARCHAR(255),
    usage_purpose        VARCHAR(255),
    sleep_time           TIME,
    wake_up_time         TIME,
    notification_setting VARCHAR(50),
    system_role          VARCHAR(20)         NOT NULL DEFAULT 'USER',
    status               VARCHAR(20)         NOT NULL DEFAULT 'ACTIVE',
    created_at           TIMESTAMP           NOT NULL,
    updated_at           TIMESTAMP,
    deleted_at           TIMESTAMP,
    created_by           UUID,
    updated_by           UUID,
    deleted_by           UUID
);

-- 2. categories 테이블
CREATE TABLE categories
(
    id         UUID PRIMARY KEY,
    user_id    UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name       VARCHAR(100) NOT NULL,
    color_code VARCHAR(50)  NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    deleted_by UUID,
    UNIQUE (user_id, name)
);

-- 3. teams 테이블
CREATE TABLE teams
(
    id          UUID PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    invite_code VARCHAR(255),
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP,
    deleted_at  TIMESTAMP,
    created_by  UUID,
    updated_by  UUID,
    deleted_by  UUID
);

-- 4. team_members 테이블
CREATE TABLE team_members
(
    id         UUID PRIMARY KEY,
    team_id    UUID        NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    team_role  VARCHAR(50) NOT NULL,
    created_at TIMESTAMP   NOT NULL,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    deleted_by UUID,
    UNIQUE (team_id, user_id)
);

-- 5. schedules 테이블
CREATE TABLE schedules
(
    id               UUID PRIMARY KEY,
    user_id          UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category_id      UUID         NOT NULL REFERENCES categories (id) ON DELETE CASCADE,
    team_id          UUID REFERENCES teams (id) ON DELETE CASCADE,
    group_id         UUID,
    repeat_pattern   VARCHAR(50),
    repeat_end_date  DATE,
    title            VARCHAR(255) NOT NULL,
    content          TEXT,
    location         VARCHAR(255),
    start_date       DATE         NOT NULL,
    start_time       TIME,
    end_date         DATE         NOT NULL,
    end_time         TIME,
    is_all_day       BOOLEAN      NOT NULL,
    status           VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    reminder_minutes INTEGER,
    created_at       TIMESTAMP    NOT NULL,
    updated_at       TIMESTAMP,
    deleted_at       TIMESTAMP,
    created_by       UUID,
    updated_by       UUID,
    deleted_by       UUID
);

-- 6. refresh_tokens 테이블 (JWT 연동용)
CREATE TABLE refresh_tokens
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    UUID      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token      TEXT      NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL
);
