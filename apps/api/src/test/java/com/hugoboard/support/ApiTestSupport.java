package com.hugoboard.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hugoboard.auth.AdminBootstrap;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Import(TestClockConfig.class)
public abstract class ApiTestSupport {

	protected static final String ADMIN_EMAIL = "admin@test.local";

	protected static final String ADMIN_PASSWORD = "test-admin-password";

	private static final String[] TABLES_IN_DELETE_ORDER = {
			"answer", "review_assignment", "form_question", "review_form",
			"auth_session", "admin_account", "team_member" };

	@Autowired
	protected WebApplicationContext context;

	@Autowired
	protected JdbcClient jdbc;

	@Autowired
	protected MutableClock clock;

	@Autowired
	protected AdminBootstrap adminBootstrap;

	protected MockMvc mvc;

	@BeforeEach
	void setUpApiTest() {
		for (String table : TABLES_IN_DELETE_ORDER) {
			jdbc.sql("DELETE FROM " + table).update();
		}
		Filter[] filters = context.getBeansOfType(Filter.class).values().toArray(Filter[]::new);
		mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(filters).build();
		clock.reset();
		adminBootstrap.sync(ADMIN_EMAIL, ADMIN_PASSWORD);
	}

	protected String adminLoginJson() throws Exception {
		return mvc.perform(post("/api/auth/admin/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json("email", ADMIN_EMAIL, "password", ADMIN_PASSWORD)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
	}

	protected String memberLoginJson(long memberId) throws Exception {
		return mvc.perform(post("/api/auth/member/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"memberId\":" + memberId + "}"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
	}

	protected String adminToken() throws Exception {
		return JsonPath.read(adminLoginJson(), "$.accessToken");
	}

	protected String memberToken(long memberId) throws Exception {
		return JsonPath.read(memberLoginJson(memberId), "$.accessToken");
	}

	protected static final String SCALE_AND_TEXT_QUESTIONS = """
			[{"text":"Mennyire segítőkész?","type":"SCALE","required":true,"scaleMin":1,"scaleMax":5},
			 {"text":"Miben fejlődhetne?","type":"TEXT","required":false}]""";

	protected static String formJson(String topic, long evaluatedMemberId, String questionsJson) {
		return "{\"topic\":\"" + topic + "\",\"evaluatedMemberId\":" + evaluatedMemberId
				+ ",\"questions\":" + questionsJson + "}";
	}

	protected long createForm(String adminToken, long evaluatedMemberId, String questionsJson) throws Exception {
		String body = mvc.perform(post("/api/admin/forms")
				.header("Authorization", bearer(adminToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content(formJson("Csapatmunka Q4", evaluatedMemberId, questionsJson)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(body, "$.id")).longValue();
	}

	protected void sendForm(String adminToken, long formId) throws Exception {
		mvc.perform(post("/api/admin/forms/" + formId + "/send").header("Authorization", bearer(adminToken)))
				.andExpect(status().isOk());
	}

	protected static String bearer(String token) {
		return "Bearer " + token;
	}

	protected static String json(String... keyValues) {
		StringBuilder sb = new StringBuilder("{");
		for (int i = 0; i < keyValues.length; i += 2) {
			if (i > 0) {
				sb.append(',');
			}
			sb.append('"').append(keyValues[i]).append("\":\"").append(keyValues[i + 1]).append('"');
		}
		return sb.append('}').toString();
	}

	protected long insertMember(String name) {
		jdbc.sql("INSERT INTO team_member (name) VALUES (:name)").param("name", name).update();
		return jdbc.sql("SELECT last_insert_rowid()").query(Long.class).single();
	}

	protected long insertMember(String name, boolean active) {
		jdbc.sql("INSERT INTO team_member (name, active) VALUES (:name, :active)")
				.param("name", name)
				.param("active", active ? 1 : 0)
				.update();
		return jdbc.sql("SELECT last_insert_rowid()").query(Long.class).single();
	}

}
