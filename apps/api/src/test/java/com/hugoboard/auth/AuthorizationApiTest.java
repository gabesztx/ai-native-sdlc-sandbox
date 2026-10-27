package com.hugoboard.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hugoboard.support.ApiTestSupport;
import java.net.URI;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class AuthorizationApiTest extends ApiTestSupport {

	// Dummy id: the role check in RoleInterceptor runs before the handler resolves the
	// path variable or body, so these requests never reach business logic.
	private static final long ANY_ID = 1;

	private static final List<Function<Long, MockHttpServletRequestBuilder>> ADMIN_ENDPOINTS = List.of(
			id -> get("/api/admin/team-members"),
			id -> get("/api/admin/forms"),
			id -> post("/api/admin/forms").contentType(MediaType.APPLICATION_JSON).content("{}"),
			id -> get("/api/admin/forms/" + id),
			id -> put("/api/admin/forms/" + id).contentType(MediaType.APPLICATION_JSON).content("{}"),
			id -> delete("/api/admin/forms/" + id),
			id -> post("/api/admin/forms/" + id + "/send"),
			id -> get("/api/admin/forms/" + id + "/responses"));

	private static final List<Function<Long, MockHttpServletRequestBuilder>> MEMBER_ENDPOINTS = List.of(
			id -> get("/api/me/reviews"),
			id -> get("/api/me/reviews/" + id),
			id -> post("/api/me/reviews/" + id + "/submit").contentType(MediaType.APPLICATION_JSON).content("{}"));

	private static final List<Function<Long, MockHttpServletRequestBuilder>> ANY_USER_ENDPOINTS = List.of(
			id -> get("/api/dashboard"),
			id -> get("/api/dashboard/issues?tile=ALL"),
			id -> get("/api/dashboard/stream"));

	static Stream<Arguments> adminEndpoints() {
		return ADMIN_ENDPOINTS.stream().map(Arguments::of);
	}

	static Stream<Arguments> memberEndpoints() {
		return MEMBER_ENDPOINTS.stream().map(Arguments::of);
	}

	static Stream<Arguments> anyUserEndpoints() {
		return ANY_USER_ENDPOINTS.stream().map(Arguments::of);
	}

	@ParameterizedTest
	@MethodSource("adminEndpoints")
	@DisplayName("AC14: belépés nélkül minden /api/admin/** végpont 401")
	void adminEndpointsRequireLogin(Function<Long, MockHttpServletRequestBuilder> endpoint) throws Exception {
		mvc.perform(endpoint.apply(ANY_ID)).andExpect(status().isUnauthorized());
	}

	@ParameterizedTest
	@MethodSource("adminEndpoints")
	@DisplayName("AC15: kollégaként minden /api/admin/** végpont 403")
	void adminEndpointsRejectMember(Function<Long, MockHttpServletRequestBuilder> endpoint) throws Exception {
		long memberId = insertMember("Teszt Elek");

		mvc.perform(endpoint.apply(ANY_ID).header(HttpHeaders.AUTHORIZATION, bearer(memberToken(memberId))))
				.andExpect(status().isForbidden());
	}

	@ParameterizedTest
	@MethodSource("memberEndpoints")
	@DisplayName("AC14: belépés nélkül minden /api/me/** végpont 401")
	void memberEndpointsRequireLogin(Function<Long, MockHttpServletRequestBuilder> endpoint) throws Exception {
		mvc.perform(endpoint.apply(ANY_ID)).andExpect(status().isUnauthorized());
	}

	@ParameterizedTest
	@MethodSource("memberEndpoints")
	@DisplayName("AC16: adminként minden /api/me/** végpont 403")
	void memberEndpointsRejectAdmin(Function<Long, MockHttpServletRequestBuilder> endpoint) throws Exception {
		mvc.perform(endpoint.apply(ANY_ID).header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
				.andExpect(status().isForbidden());
	}

	@ParameterizedTest
	@MethodSource("anyUserEndpoints")
	@DisplayName("AC24: belépés nélkül minden /api/dashboard** végpont 401")
	void dashboardEndpointsRequireLogin(Function<Long, MockHttpServletRequestBuilder> endpoint) throws Exception {
		mvc.perform(endpoint.apply(ANY_ID)).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("AC25: a /api/dashboard sem admin-, sem kolléga-specifikus — kollégaként is 200, nem 403")
	void dashboardEndpointAcceptsMemberWithoutRoleCheck() throws Exception {
		long memberId = insertMember("Teszt Elek");

		mvc.perform(get("/api/dashboard").header(HttpHeaders.AUTHORIZATION, bearer(memberToken(memberId))))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("AC15: adminként az admin végpont elérhető")
	void adminCanUseAdminEndpoint() throws Exception {
		mvc.perform(get("/api/admin/team-members")
				.header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("Kódolt vagy mátrixparaméteres admin útvonal is 401 belépés nélkül")
	void encodedAdminPathStillRequiresLogin() throws Exception {
		mvc.perform(get(URI.create("/api/%61dmin/forms"))).andExpect(status().isUnauthorized());
		mvc.perform(get(URI.create("/api/admin;x=1/forms"))).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("Kódolt admin útvonalon kollégaként is 403, nem 200")
	void encodedAdminPathStillRejectsMember() throws Exception {
		long memberId = insertMember("Minta Anna");

		mvc.perform(get(URI.create("/api/%61dmin/forms"))
				.header(HttpHeaders.AUTHORIZATION, bearer(memberToken(memberId))))
				.andExpect(status().isForbidden());
	}

}
