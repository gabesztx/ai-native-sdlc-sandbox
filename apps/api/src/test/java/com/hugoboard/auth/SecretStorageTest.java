package com.hugoboard.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.hugoboard.support.ApiTestSupport;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SecretStorageTest extends ApiTestSupport {

	@Test
	@DisplayName("AC12: az auth_session táblában nincs nyílt token")
	void tokensAreStoredHashed() throws Exception {
		String login = adminLoginJson();
		String access = JsonPath.read(login, "$.accessToken");
		String refresh = JsonPath.read(login, "$.refreshToken");

		List<String> stored = jdbc.sql("SELECT access_token_hash || ' ' || refresh_token_hash FROM auth_session")
				.query(String.class).list();

		assertThat(stored).isNotEmpty();
		assertThat(stored).noneMatch(row -> row.contains(access) || row.contains(refresh));
	}

	@Test
	@DisplayName("AC13: az admin_account táblában nincs nyílt jelszó")
	void passwordIsStoredHashed() {
		String stored = jdbc.sql("SELECT password_hash FROM admin_account").query(String.class).single();

		assertThat(stored).isNotEqualTo(ADMIN_PASSWORD).doesNotContain(ADMIN_PASSWORD).startsWith("$2");
	}

}
