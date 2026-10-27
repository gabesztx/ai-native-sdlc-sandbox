package com.hugoboard.dashboard;

import com.hugoboard.common.Timestamps;
import com.hugoboard.jira.JiraClient;
import com.hugoboard.jira.JiraImportException;
import com.hugoboard.jira.JiraIssue;
import com.hugoboard.jira.JiraNoActiveSprintException;
import com.hugoboard.jira.JiraProperties;
import com.hugoboard.jira.JiraRateLimitException;
import com.hugoboard.jira.JiraSprint;
import com.hugoboard.jira.JiraStatus;
import com.hugoboard.jira.JiraUser;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * One poll cycle. Every Jira call that can fail happens before any database write, so an
 * error anywhere leaves team_member/jira_issue/jira_sprint/jira_sync_state exactly as they
 * were (AC16) — there is no partial-write state to roll back.
 */
@Service
class DashboardSyncService {

	private static final Logger log = LoggerFactory.getLogger(DashboardSyncService.class);

	private final DashboardRepository repository;

	private final JiraClient jiraClient;

	private final DashboardEventBroadcaster broadcaster;

	private final DashboardService dashboardService;

	private final JiraProperties jiraProperties;

	private final Clock clock;

	private volatile Instant backOffUntil = Instant.MIN;

	DashboardSyncService(DashboardRepository repository, JiraClient jiraClient,
			DashboardEventBroadcaster broadcaster, DashboardService dashboardService,
			JiraProperties jiraProperties, Clock clock) {
		this.repository = repository;
		this.jiraClient = jiraClient;
		this.broadcaster = broadcaster;
		this.dashboardService = dashboardService;
		this.jiraProperties = jiraProperties;
		this.clock = clock;
	}

	@Transactional
	public void syncNow() {
		if (!jiraProperties.isConfigured() || clock.instant().isBefore(backOffUntil)) {
			return;
		}
		JiraSprint activeSprint;
		try {
			activeSprint = jiraClient.findActiveSprint();
		} catch (JiraRateLimitException e) {
			backOff(e);
			return;
		} catch (JiraNoActiveSprintException e) {
			repository.findActiveSprint().ifPresent(current -> repository.markSprintClosed(current.id()));
			log.info("Dashboard sync: {}", e.getMessage());
			return;
		} catch (JiraImportException e) {
			log.error("Dashboard sync failed while looking up the active sprint: {}", e.getMessage());
			return;
		}

		Optional<SprintRow> currentActive = repository.findActiveSprint();
		boolean changed;
		try {
			changed = currentActive.isEmpty() || currentActive.get().id() != activeSprint.id()
					? fullReload(activeSprint, currentActive.orElse(null))
					: incrementalSync(activeSprint, currentActive.get());
		} catch (JiraRateLimitException e) {
			backOff(e);
			return;
		} catch (JiraImportException e) {
			log.error("Dashboard sync failed: {}", e.getMessage());
			return;
		}

		repository.updateLastSyncedAt(Timestamps.now(clock));
		if (changed) {
			broadcaster.broadcast(dashboardService.getDashboard());
		}
	}

	private void backOff(JiraRateLimitException e) {
		backOffUntil = clock.instant().plus(e.retryAfter());
		log.warn("Dashboard sync rate-limited by Jira, backing off until {}: {}", backOffUntil, e.getMessage());
	}

	/** Test seam: backOffUntil is singleton state that would otherwise leak across tests sharing a cached context. */
	void resetBackOff() {
		backOffUntil = Instant.MIN;
	}

	private boolean fullReload(JiraSprint activeSprint, SprintRow previousActive) {
		List<JiraIssue> issues = jiraClient.fetchActiveSprintIssues(activeSprint.id());
		repository.upsertSprint(toSprintRow(activeSprint));
		if (previousActive != null && previousActive.id() != activeSprint.id()) {
			repository.markSprintClosed(previousActive.id());
		}
		for (JiraIssue issue : issues) {
			repository.upsertIssue(toIssueRow(issue, activeSprint.id()));
		}
		return true;
	}

	private boolean incrementalSync(JiraSprint activeSprint, SprintRow previousActive) {
		String lastSyncedAt = repository.findLastSyncedAt();
		List<JiraIssue> changedIssues = lastSyncedAt == null
				? jiraClient.fetchActiveSprintIssues(activeSprint.id())
				: jiraClient.fetchIssuesUpdatedSince(activeSprint.id(), lastSyncedAt);
		Set<String> liveKeys = new HashSet<>(jiraClient.fetchActiveSprintIssueKeys(activeSprint.id()));
		List<String> storedKeys = repository.findIssueKeysBySprint(activeSprint.id());
		List<String> missingKeys = storedKeys.stream().filter(key -> !liveKeys.contains(key)).toList();

		boolean sprintFieldsChanged = !Objects.equals(previousActive.name(), activeSprint.name())
				|| !Objects.equals(previousActive.startDate(), activeSprint.startDate())
				|| !Objects.equals(previousActive.endDate(), activeSprint.endDate());
		repository.upsertSprint(toSprintRow(activeSprint));

		for (JiraIssue issue : changedIssues) {
			repository.upsertIssue(toIssueRow(issue, activeSprint.id()));
		}
		for (String key : missingKeys) {
			if (jiraClient.issueExists(key)) {
				repository.clearSprintId(key);
			} else {
				repository.deleteIssueByKey(key);
			}
		}
		return sprintFieldsChanged || !changedIssues.isEmpty() || !missingKeys.isEmpty();
	}

	private static SprintRow toSprintRow(JiraSprint sprint) {
		return new SprintRow(sprint.id(), sprint.name(), "active", sprint.startDate(), sprint.endDate());
	}

	private static IssueRow toIssueRow(JiraIssue issue, long sprintId) {
		JiraStatus status = issue.status();
		String statusCategoryKey = status == null || status.statusCategory() == null
				? null
				: status.statusCategory().key();
		JiraUser assignee = issue.assignee();
		return new IssueRow(
				issue.id(),
				issue.key(),
				sprintId,
				issue.summary(),
				status == null ? null : status.name(),
				StatusCategoryMapper.map(statusCategoryKey),
				assignee == null ? null : assignee.accountId(),
				assignee == null ? null : assignee.displayName(),
				assignee == null ? null : assignee.avatarUrl(),
				issue.updated());
	}

}
