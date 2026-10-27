package com.hugoboard.dashboard;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * A thin wrapper — all logic lives in DashboardSyncService.syncNow(), which tests call
 * directly instead of waiting for this timer. fixedDelay (not fixedRate) alone guarantees a
 * poll never overlaps the previous one (AC8): Spring starts the next wait only after the
 * previous invocation returns.
 *
 * <p>initialDelay equals the poll interval (rather than firing immediately, Spring's
 * default for fixedDelay) so the first poll runs on this bean's own background thread only
 * after application startup has fully settled, never racing the main-thread
 * ApplicationRunners (e.g. JiraTeamImportRunner) for the same mocked Jira server in tests.
 */
@Component
class DashboardSyncScheduler {

	private final DashboardSyncService syncService;

	DashboardSyncScheduler(DashboardSyncService syncService) {
		this.syncService = syncService;
	}

	@Scheduled(initialDelayString = "${hugo.dashboard.poll-interval-ms}",
			fixedDelayString = "${hugo.dashboard.poll-interval-ms}")
	void poll() {
		syncService.syncNow();
	}

}
