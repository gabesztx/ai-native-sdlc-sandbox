package com.hugoboard.dashboard;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
class DashboardService {

	private final DashboardRepository repository;

	private final Clock clock;

	private final String jiraBaseUrl;

	DashboardService(DashboardRepository repository, Clock clock,
			@Value("${hugo.jira.base-url:}") String jiraBaseUrl) {
		this.repository = repository;
		this.clock = clock;
		this.jiraBaseUrl = jiraBaseUrl;
	}

	/**
	 * updatedAt is last_synced_at, not "now" — null means no sync has ever succeeded, which
	 * is how the frontend tells "Jira unavailable, no data at all yet" (AC23) apart from
	 * "synced fine, there's just no active sprint right now" (AC4).
	 */
	Dashboard getDashboard() {
		Optional<SprintRow> activeSprint = repository.findActiveSprint();
		List<IssueRow> issues = issuesOf(activeSprint);
		Sprint sprint = activeSprint.map(this::toSprint).orElse(null);
		return new Dashboard(sprint, computeKpis(issues), repository.findLastSyncedAt());
	}

	List<Issue> getIssues(Tile tile) {
		List<IssueRow> issues = issuesOf(repository.findActiveSprint());
		return issues.stream()
				.filter(issue -> matchesTile(issue, tile))
				.map(this::toIssue)
				.toList();
	}

	private List<IssueRow> issuesOf(Optional<SprintRow> activeSprint) {
		return activeSprint.map(sprint -> repository.findIssuesBySprint(sprint.id())).orElse(List.of());
	}

	private static boolean matchesTile(IssueRow issue, Tile tile) {
		return switch (tile) {
			case ALL -> true;
			case DONE -> "DONE".equals(issue.statusCategory());
			case IN_PROGRESS -> "IN_PROGRESS".equals(issue.statusCategory());
			case BLOCKED -> "Blocked".equals(issue.statusName());
			case UNASSIGNED -> issue.assigneeAccountId() == null;
		};
	}

	private static Dashboard.Kpis computeKpis(List<IssueRow> issues) {
		int total = issues.size();
		long done = issues.stream().filter(i -> "DONE".equals(i.statusCategory())).count();
		long inProgress = issues.stream().filter(i -> "IN_PROGRESS".equals(i.statusCategory())).count();
		long blocked = issues.stream().filter(i -> "Blocked".equals(i.statusName())).count();
		long unassigned = issues.stream().filter(i -> i.assigneeAccountId() == null).count();
		int donePercent = total == 0 ? 0 : Math.round(done * 100f / total);
		return new Dashboard.Kpis(total, donePercent, (int) inProgress, (int) blocked, (int) unassigned);
	}

	private Sprint toSprint(SprintRow row) {
		int elapsedPercent = 0;
		int daysRemaining = 0;
		if (row.startDate() != null && row.endDate() != null) {
			Instant now = clock.instant();
			Instant start = Instant.parse(row.startDate());
			Instant end = Instant.parse(row.endDate());
			long totalMinutes = Duration.between(start, end).toMinutes();
			if (totalMinutes > 0) {
				long elapsedMinutes = Duration.between(start, now).toMinutes();
				elapsedPercent = clamp(Math.round(elapsedMinutes * 100f / totalMinutes), 0, 100);
			}
			long remainingMinutes = Duration.between(now, end).toMinutes();
			daysRemaining = Math.max(0, (int) Math.ceil(remainingMinutes / (60.0 * 24)));
		}
		return new Sprint(row.name(), row.startDate(), row.endDate(), elapsedPercent, daysRemaining);
	}

	private Issue toIssue(IssueRow row) {
		Issue.Assignee assignee = row.assigneeAccountId() == null
				? null
				: new Issue.Assignee(row.assigneeName(), row.assigneeAvatarUrl());
		String url = jiraBaseUrl.isBlank() ? "" : jiraBaseUrl + "/browse/" + row.issueKey();
		return new Issue(row.issueKey(), row.summary(), row.statusName(), row.statusCategory(), assignee, url);
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

}
