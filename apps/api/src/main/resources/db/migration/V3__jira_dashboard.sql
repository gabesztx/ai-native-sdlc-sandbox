CREATE TABLE jira_sprint (
    id         INTEGER PRIMARY KEY,
    name       TEXT    NOT NULL,
    state      TEXT    NOT NULL CHECK (state IN ('active', 'closed', 'future')),
    start_date TEXT,
    end_date   TEXT
);

CREATE TABLE jira_issue (
    id                   TEXT    PRIMARY KEY,
    issue_key            TEXT    NOT NULL,
    sprint_id            INTEGER REFERENCES jira_sprint (id),
    summary              TEXT    NOT NULL,
    status_name          TEXT    NOT NULL,
    status_category      TEXT    NOT NULL CHECK (status_category IN ('TODO', 'IN_PROGRESS', 'DONE')),
    assignee_account_id  TEXT,
    assignee_name        TEXT,
    assignee_avatar_url  TEXT,
    jira_updated_at      TEXT    NOT NULL
);

CREATE UNIQUE INDEX jira_issue_issue_key_idx ON jira_issue (issue_key);

CREATE TABLE jira_sync_state (
    id             INTEGER PRIMARY KEY CHECK (id = 1),
    last_synced_at TEXT
);

INSERT INTO jira_sync_state (id, last_synced_at) VALUES (1, NULL);
