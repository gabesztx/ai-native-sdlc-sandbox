package com.hugoboard.support;

import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
public abstract class ApiTestSupport {

	private static final String[] TABLES_IN_DELETE_ORDER = {
			"answer", "review_assignment", "form_question", "review_form",
			"auth_session", "admin_account", "team_member" };

	@Autowired
	protected WebApplicationContext context;

	@Autowired
	protected JdbcClient jdbc;

	protected MockMvc mvc;

	@BeforeEach
	void setUpApiTest() {
		for (String table : TABLES_IN_DELETE_ORDER) {
			jdbc.sql("DELETE FROM " + table).update();
		}
		Filter[] filters = context.getBeansOfType(Filter.class).values().toArray(Filter[]::new);
		mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(filters).build();
	}

	protected long insertMember(String name) {
		jdbc.sql("INSERT INTO team_member (name) VALUES (:name)").param("name", name).update();
		return jdbc.sql("SELECT last_insert_rowid()").query(Long.class).single();
	}

}
