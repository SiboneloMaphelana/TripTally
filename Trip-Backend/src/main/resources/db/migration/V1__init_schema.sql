CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE trips (
    id BIGSERIAL PRIMARY KEY,
    owner_user_id BIGINT NOT NULL REFERENCES users (id),
    title VARCHAR(200) NOT NULL,
    destination VARCHAR(300) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    cover_image_path VARCHAR(500),
    currency_code VARCHAR(3) NOT NULL DEFAULT 'ZAR',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE trip_members (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT NOT NULL REFERENCES trips (id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users (id),
    display_name VARCHAR(120) NOT NULL,
    invited_email VARCHAR(255),
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT trip_members_role_chk CHECK (role IN ('OWNER', 'MEMBER'))
);

CREATE UNIQUE INDEX ux_trip_members_trip_user ON trip_members (trip_id, user_id) WHERE user_id IS NOT NULL;

CREATE TABLE expenses (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT NOT NULL REFERENCES trips (id) ON DELETE CASCADE,
    payer_member_id BIGINT NOT NULL REFERENCES trip_members (id),
    amount NUMERIC(19, 2) NOT NULL,
    category VARCHAR(40) NOT NULL,
    description VARCHAR(500) NOT NULL,
    expense_date DATE NOT NULL,
    split_mode VARCHAR(20) NOT NULL,
    settled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE expense_participants (
    id BIGSERIAL PRIMARY KEY,
    expense_id BIGINT NOT NULL REFERENCES expenses (id) ON DELETE CASCADE,
    trip_member_id BIGINT NOT NULL REFERENCES trip_members (id),
    owed_amount NUMERIC(19, 2) NOT NULL,
    split_input NUMERIC(19, 6) NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX ux_expense_participant ON expense_participants (expense_id, trip_member_id);

CREATE TABLE settlements (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT NOT NULL REFERENCES trips (id) ON DELETE CASCADE,
    from_member_id BIGINT NOT NULL REFERENCES trip_members (id),
    to_member_id BIGINT NOT NULL REFERENCES trip_members (id),
    amount NUMERIC(19, 2) NOT NULL,
    note VARCHAR(500),
    recorded_by_user_id BIGINT NOT NULL REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE receipt_attachments (
    id BIGSERIAL PRIMARY KEY,
    expense_id BIGINT NOT NULL UNIQUE REFERENCES expenses (id) ON DELETE CASCADE,
    file_path VARCHAR(500) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_expenses_trip ON expenses (trip_id);
CREATE INDEX ix_expenses_trip_date ON expenses (trip_id, expense_date);
CREATE INDEX ix_settlements_trip ON settlements (trip_id);
