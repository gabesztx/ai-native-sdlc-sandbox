package com.hugoboard.auth;

import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AdminAccountRepository {

	private final JdbcClient jdbc;

	public AdminAccountRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	Optional<AdminAccount> findByEmail(String email) {
		return jdbc.sql("""
				SELECT id, email, password_hash
				FROM admin_account
				WHERE email = :email
				""")
				.param("email", email)
				.query(AdminAccount.class)
				.optional();
	}

	Optional<AdminAccount> findFirst() {
		return jdbc.sql("""
				SELECT id, email, password_hash
				FROM admin_account
				ORDER BY id
				LIMIT 1
				""")
				.query(AdminAccount.class)
				.optional();
	}

	void insert(String email, String passwordHash) {
		jdbc.sql("""
				INSERT INTO admin_account (email, password_hash)
				VALUES (:email, :passwordHash)
				""")
				.param("email", email)
				.param("passwordHash", passwordHash)
				.update();
	}

	void update(long id, String email, String passwordHash) {
		jdbc.sql("""
				UPDATE admin_account
				SET email = :email, password_hash = :passwordHash
				WHERE id = :id
				""")
				.param("id", id)
				.param("email", email)
				.param("passwordHash", passwordHash)
				.update();
	}

	void deleteAllExcept(long id) {
		jdbc.sql("DELETE FROM admin_account WHERE id <> :id")
				.param("id", id)
				.update();
	}

	void deleteAll() {
		jdbc.sql("DELETE FROM admin_account").update();
	}

}
