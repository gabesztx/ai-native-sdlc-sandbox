package com.hugoboard.user;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class JiraTeamImportRunner implements ApplicationRunner {

	private final JiraTeamImportService importService;

	public JiraTeamImportRunner(JiraTeamImportService importService) {
		this.importService = importService;
	}

	@Override
	public void run(ApplicationArguments args) {
		importService.importIfNeeded();
	}

}
