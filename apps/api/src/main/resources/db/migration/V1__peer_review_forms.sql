CREATE TABLE team_member (
    id   INTEGER PRIMARY KEY,
    name TEXT    NOT NULL
);

CREATE TABLE admin_account (
    id            INTEGER PRIMARY KEY,
    email         TEXT    NOT NULL UNIQUE,
    password_hash TEXT    NOT NULL
);

CREATE TABLE auth_session (
    id                 INTEGER PRIMARY KEY,
    role               TEXT    NOT NULL CHECK (role IN ('ADMIN', 'MEMBER')),
    member_id          INTEGER REFERENCES team_member (id),
    access_token_hash  TEXT    NOT NULL UNIQUE,
    access_expires_at  TEXT    NOT NULL,
    refresh_token_hash TEXT    NOT NULL UNIQUE,
    refresh_expires_at TEXT    NOT NULL,
    revoked_at         TEXT,
    created_at         TEXT    NOT NULL
);

CREATE TABLE review_form (
    id                  INTEGER PRIMARY KEY,
    topic               TEXT    NOT NULL,
    evaluated_member_id INTEGER NOT NULL REFERENCES team_member (id),
    status              TEXT    NOT NULL CHECK (status IN ('DRAFT', 'SENT')),
    created_at          TEXT    NOT NULL,
    sent_at             TEXT
);

CREATE TABLE form_question (
    id        INTEGER PRIMARY KEY,
    form_id   INTEGER NOT NULL REFERENCES review_form (id) ON DELETE CASCADE,
    position  INTEGER NOT NULL,
    text      TEXT    NOT NULL,
    type      TEXT    NOT NULL CHECK (type IN ('SCALE', 'TEXT')),
    required  INTEGER NOT NULL CHECK (required IN (0, 1)),
    scale_min INTEGER,
    scale_max INTEGER,
    UNIQUE (form_id, position),
    CHECK (
        (type = 'SCALE' AND scale_min IS NOT NULL AND scale_max IS NOT NULL AND scale_max > scale_min)
        OR (type = 'TEXT' AND scale_min IS NULL AND scale_max IS NULL)
    )
);

CREATE TABLE review_assignment (
    id                  INTEGER PRIMARY KEY,
    form_id             INTEGER NOT NULL REFERENCES review_form (id),
    recipient_member_id INTEGER NOT NULL REFERENCES team_member (id),
    status              TEXT    NOT NULL CHECK (status IN ('PENDING', 'SUBMITTED')),
    submitted_at        TEXT,
    UNIQUE (form_id, recipient_member_id)
);

CREATE TABLE answer (
    id            INTEGER PRIMARY KEY,
    assignment_id INTEGER NOT NULL REFERENCES review_assignment (id),
    question_id   INTEGER NOT NULL REFERENCES form_question (id),
    scale_value   INTEGER,
    text_value    TEXT,
    UNIQUE (assignment_id, question_id)
);
