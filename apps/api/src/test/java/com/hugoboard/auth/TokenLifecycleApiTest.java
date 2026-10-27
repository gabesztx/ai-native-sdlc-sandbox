package com.hugoboard.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hugoboard.support.ApiTestSupport;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class TokenLifecycleApiTest extends ApiTestSupport {

	@Test
	@DisplayName("AC7: kilépés után a régi access és refresh token is 401")
	void logoutRevokesBothTokens() throws Exception {
		String login = adminLoginJson();
		String access = JsonPath.read(login, "$.accessToken");
		String refresh = JsonPath.read(login, "$.refreshToken");

		mvc.perform(post("/api/auth/logout")
				.header(HttpHeaders.AUTHORIZATION, bearer(access))
				.contentType(MediaType.APPLICATION_JSON)
				.content(json("refreshToken", refresh)))
				.andExpect(status().isNoContent());

		me(access).andExpect(status().isUnauthorized());
		refresh(refresh).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("AC8: a belépési válaszban két token és élettartam van, süti nincs")
	void loginReturnsTokensWithoutCookies() throws Exception {
		long memberId = insertMember("Teszt Elek");

		mvc.perform(post("/api/auth/member/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"memberId\":" + memberId + "}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isString())
				.andExpect(jsonPath("$.refreshToken").isString())
				.andExpect(jsonPath("$.accessTokenExpiresIn").value(900))
				.andExpect(jsonPath("$.refreshTokenExpiresIn").value(28800))
				.andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
	}

	@Test
	@DisplayName("AC9: refresh után az új pár működik, a régi refresh token 401")
	void refreshRotatesTokens() throws Exception {
		String login = adminLoginJson();
		String oldRefresh = JsonPath.read(login, "$.refreshToken");

		String refreshed = refresh(oldRefresh)
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		String newAccess = JsonPath.read(refreshed, "$.accessToken");

		me(newAccess).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN"));
		refresh(oldRefresh).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("AC10: lejárt access tokennel a védett végpont 401")
	void expiredAccessTokenIsRejected() throws Exception {
		String access = adminToken();
		clock.advance(Duration.ofMinutes(15).plusSeconds(1));

		me(access).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("AC11: lejárt refresh tokennel a frissítés 401")
	void expiredRefreshTokenIsRejected() throws Exception {
		String refresh = JsonPath.read(adminLoginJson(), "$.refreshToken");
		clock.advance(Duration.ofHours(8).plusSeconds(1));

		refresh(refresh).andExpect(status().isUnauthorized());
	}

	private ResultActions me(String accessToken) throws Exception {
		return mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(accessToken)));
	}

	private ResultActions refresh(String refreshToken) throws Exception {
		return mvc.perform(post("/api/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json("refreshToken", refreshToken)));
	}

}
