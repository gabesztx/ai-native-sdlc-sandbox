package com.hugoboard.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hugoboard.support.ApiTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class AdminLoginApiTest extends ApiTestSupport {

	@Test
	@DisplayName("AC2: helyes e-mail és jelszó után a me válaszban ADMIN szerepkör áll")
	void adminLogsInWithConfiguredCredentials() throws Exception {
		login(ADMIN_EMAIL, ADMIN_PASSWORD)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.me.role").value("ADMIN"));
	}

	@Test
	@DisplayName("AC3: a környezeti jelszó cseréje után a régi jelszó 401")
	void changedConfiguredPasswordInvalidatesOldOne() throws Exception {
		adminBootstrap.sync(ADMIN_EMAIL, "new-admin-password");

		login(ADMIN_EMAIL, ADMIN_PASSWORD).andExpect(status().isUnauthorized());
		login(ADMIN_EMAIL, "new-admin-password").andExpect(status().isOk());
	}

	@Test
	@DisplayName("AC3: hiányzó környezeti adatokkal nincs admin belépés")
	void missingConfigurationDisablesAdminLogin() throws Exception {
		adminBootstrap.sync("", "");

		login(ADMIN_EMAIL, ADMIN_PASSWORD).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("AC4: hibás jelszó és nem létező e-mail egyaránt 401, ugyanazzal az üzenettel")
	void wrongPasswordAndUnknownEmailLookTheSame() throws Exception {
		String wrongPassword = login(ADMIN_EMAIL, "wrong-password")
				.andExpect(status().isUnauthorized())
				.andReturn().getResponse().getContentAsString();
		String unknownEmail = login("nobody@test.local", ADMIN_PASSWORD)
				.andExpect(status().isUnauthorized())
				.andReturn().getResponse().getContentAsString();

		org.assertj.core.api.Assertions.assertThat(wrongPassword).isEqualTo(unknownEmail);
	}

	private ResultActions login(String email, String password) throws Exception {
		return mvc.perform(post("/api/auth/admin/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json("email", email, "password", password)));
	}

}
