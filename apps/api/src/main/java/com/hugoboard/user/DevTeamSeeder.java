package com.hugoboard.user;

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

	public DevTeamSeeder(TeamMemberRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (repository.count() == 0) {
			DEMO_NAMES.forEach(repository::insert);
		}
	}

}
