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

CREATE TABLE teams
(
    id          UUID PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    invite_code VARCHAR(255) NOT NULL UNIQUE,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP,
    deleted_at  TIMESTAMP,
    created_by  UUID,
    updated_by  UUID,
    deleted_by  UUID
);

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

CREATE TABLE refresh_tokens
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    UUID      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token      TEXT      NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_team_members_user_id ON team_members (user_id);
CREATE INDEX idx_schedules_user_id ON schedules (user_id);
CREATE INDEX idx_schedules_category_id ON schedules (category_id);
CREATE INDEX idx_schedules_team_id ON schedules (team_id);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
