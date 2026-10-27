package com.hugoboard.user;

import com.hugoboard.jira.JiraProperties;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
public class DevTeamSeeder implements ApplicationRunner {

	private static final List<String> DEMO_NAMES = List.of(
			"Teszt Elek", "Minta Anna", "Próba Péter", "Kitalált Kata", "Példa Pál");

	private final TeamMemberRepository repository;

	private final JiraProperties jiraProperties;

	public DevTeamSeeder(TeamMemberRepository repository, JiraProperties jiraProperties) {
		this.repository = repository;
		this.jiraProperties = jiraProperties;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		// A configured Jira connection always wins: fake and real team members must
		// never end up in the same database.
		if (jiraProperties.isConfigured()) {
			return;
		}
		if (repository.count() == 0) {
			DEMO_NAMES.forEach(repository::insert);
		}
	}

}
