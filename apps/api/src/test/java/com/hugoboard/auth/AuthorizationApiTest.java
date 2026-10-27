package com.hugoboard.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hugoboard.support.ApiTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class AuthorizationApiTest extends ApiTestSupport {

	@Test
	@DisplayName("AC14: belépés nélkül az admin végpont 401")
	void adminEndpointRequiresLogin() throws Exception {
		mvc.perform(get("/api/admin/team-members")).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("AC15: kollégaként az admin végpont 403")
	void memberCannotUseAdminEndpoint() throws Exception {
		long memberId = insertMember("Teszt Elek");

		mvc.perform(get("/api/admin/team-members")
				.header(HttpHeaders.AUTHORIZATION, bearer(memberToken(memberId))))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("AC15: adminként az admin végpont elérhető")
	void adminCanUseAdminEndpoint() throws Exception {
		mvc.perform(get("/api/admin/team-members")
				.header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
				.andExpect(status().isOk());
	}

}
