ALTER TABLE team_member ADD COLUMN jira_account_id TEXT;
ALTER TABLE team_member ADD COLUMN email TEXT;
ALTER TABLE team_member ADD COLUMN avatar_url TEXT;
ALTER TABLE team_member ADD COLUMN active INTEGER NOT NULL DEFAULT 1;
ALTER TABLE team_member ADD COLUMN imported_at TEXT;

CREATE UNIQUE INDEX team_member_jira_account_id_idx
    ON team_member (jira_account_id)
    WHERE jira_account_id IS NOT NULL;
