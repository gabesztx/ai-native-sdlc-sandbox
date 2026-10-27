package com.hugoboard.dashboard;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class DashboardRepository {

	private final JdbcClient jdbc;

	DashboardRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	Optional<SprintRow> findActiveSprint() {
		return jdbc.sql("""
				SELECT id, name, state, start_date, end_date
				FROM jira_sprint
				WHERE state = 'active'
				""")
				.query(SprintRow.class)
				.optional();
	}

	void upsertSprint(SprintRow sprint) {
		jdbc.sql("""
				INSERT INTO jira_sprint (id, name, state, start_date, end_date)
				VALUES (:id, :name, :state, :startDate, :endDate)
				ON CONFLICT (id) DO UPDATE SET
					name = excluded.name,
					state = excluded.state,
					start_date = excluded.start_date,
					end_date = excluded.end_date
				""")
				.param("id", sprint.id())
				.param("name", sprint.name())
				.param("state", sprint.state())
				.param("startDate", sprint.startDate())
				.param("endDate", sprint.endDate())
				.update();
	}

	void markSprintClosed(long sprintId) {
		jdbc.sql("UPDATE jira_sprint SET state = 'closed' WHERE id = :id")
				.param("id", sprintId)
				.update();
	}

	void upsertIssue(IssueRow issue) {
		jdbc.sql("""
				INSERT INTO jira_issue (id, issue_key, sprint_id, summary, status_name, status_category,
					assignee_account_id, assignee_name, assignee_avatar_url, jira_updated_at)
				VALUES (:id, :issueKey, :sprintId, :summary, :statusName, :statusCategory,
					:assigneeAccountId, :assigneeName, :assigneeAvatarUrl, :jiraUpdatedAt)
				ON CONFLICT (id) DO UPDATE SET
					issue_key = excluded.issue_key,
					sprint_id = excluded.sprint_id,
					summary = excluded.summary,
					status_name = excluded.status_name,
					status_category = excluded.status_category,
					assignee_account_id = excluded.assignee_account_id,
					assignee_name = excluded.assignee_name,
					assignee_avatar_url = excluded.assignee_avatar_url,
					jira_updated_at = excluded.jira_updated_at
				WHERE excluded.jira_updated_at >= jira_issue.jira_updated_at
				""")
				.param("id", issue.id())
				.param("issueKey", issue.issueKey())
				.param("sprintId", issue.sprintId())
				.param("summary", issue.summary())
				.param("statusName", issue.statusName())
				.param("statusCategory", issue.statusCategory())
				.param("assigneeAccountId", issue.assigneeAccountId())
				.param("assigneeName", issue.assigneeName())
				.param("assigneeAvatarUrl", issue.assigneeAvatarUrl())
				.param("jiraUpdatedAt", issue.jiraUpdatedAt())
				.update();
	}

	List<String> findIssueKeysBySprint(long sprintId) {
		return jdbc.sql("SELECT issue_key FROM jira_issue WHERE sprint_id = :sprintId")
				.param("sprintId", sprintId)
				.query(String.class)
				.list();
	}

	void clearSprintId(String issueKey) {
		jdbc.sql("UPDATE jira_issue SET sprint_id = NULL WHERE issue_key = :issueKey")
				.param("issueKey", issueKey)
				.update();
	}

	void deleteIssueByKey(String issueKey) {
		jdbc.sql("DELETE FROM jira_issue WHERE issue_key = :issueKey")
				.param("issueKey", issueKey)
				.update();
	}

	String findLastSyncedAt() {
		// .list().get(0), not .single(): the latter rejects a null result outright
		// (DataAccessUtils.requiredSingleResult), and last_synced_at starts out NULL.
		return jdbc.sql("SELECT last_synced_at FROM jira_sync_state WHERE id = 1")
				.query((rs, rowNum) -> rs.getString("last_synced_at"))
				.list()
				.get(0);
	}

	void updateLastSyncedAt(String timestamp) {
		jdbc.sql("UPDATE jira_sync_state SET last_synced_at = :timestamp WHERE id = 1")
				.param("timestamp", timestamp)
				.update();
	}

	List<IssueRow> findIssuesBySprint(long sprintId) {
		return jdbc.sql("""
				SELECT id, issue_key, sprint_id, summary, status_name, status_category,
					assignee_account_id, assignee_name, assignee_avatar_url, jira_updated_at
				FROM jira_issue
				WHERE sprint_id = :sprintId
				ORDER BY issue_key
				""")
				.param("sprintId", sprintId)
				.query(IssueRow.class)
				.list();
	}

}
