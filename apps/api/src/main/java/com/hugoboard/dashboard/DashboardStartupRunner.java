package com.hugoboard.dashboard;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Runs the first dashboard sync synchronously, on the main startup thread, right away
 * (AC22/AC23) — deliberately not left to DashboardSyncScheduler's first firing, which is
 * delayed by a full poll interval precisely so it never races this (and other)
 * ApplicationRunners for the same mocked Jira server in tests.
 */
@Component
class DashboardStartupRunner implements ApplicationRunner {

	private final DashboardSyncService syncService;

	DashboardStartupRunner(DashboardSyncService syncService) {
		this.syncService = syncService;
	}

	@Override
	public void run(ApplicationArguments args) {
		syncService.syncNow();
	}

}
