package com.hugoboard.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrap implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

	private final AdminAccountRepository repository;

	private final BCryptPasswordEncoder passwordEncoder;

	private final String configuredEmail;

	private final String configuredPassword;

	public AdminBootstrap(AdminAccountRepository repository, BCryptPasswordEncoder passwordEncoder,
			@Value("${hugo.admin.email:}") String configuredEmail,
			@Value("${hugo.admin.password:}") String configuredPassword) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
		this.configuredEmail = configuredEmail;
		this.configuredPassword = configuredPassword;
	}

	@Override
	public void run(ApplicationArguments args) {
		sync(configuredEmail, configuredPassword);
	}

	@Transactional
	public void sync(String email, String password) {
		if (email == null || email.isBlank() || password == null || password.isBlank()) {
			repository.deleteAll();
			log.warn("Admin login is disabled: HUGO_ADMIN_EMAIL and HUGO_ADMIN_PASSWORD are not set");
			return;
		}
		var existing = repository.findFirst();
		if (existing.isEmpty()) {
			repository.insert(email, passwordEncoder.encode(password));
			return;
		}
		AdminAccount admin = existing.get();
		boolean unchanged = admin.email().equals(email)
				&& passwordEncoder.matches(password, admin.passwordHash());
		if (!unchanged) {
			repository.update(admin.id(), email, passwordEncoder.encode(password));
		}
		repository.deleteAllExcept(admin.id());
	}

}
