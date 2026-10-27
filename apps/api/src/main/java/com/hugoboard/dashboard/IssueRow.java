package com.hugoboard.dashboard;

record IssueRow(
		String id,
		String issueKey,
		Long sprintId,
		String summary,
		String statusName,
		String statusCategory,
		String assigneeAccountId,
		String assigneeName,
		String assigneeAvatarUrl,
		String jiraUpdatedAt) {
}
