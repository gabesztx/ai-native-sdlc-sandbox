package com.hugoboard.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

// Its own Spring context (different hugo.admin.* properties than the shared test
// context), so AdminBootstrap.run() executes with real @Value-injected properties at
// startup -- not via a test calling adminBootstrap.sync(...) directly, which every
// other test in this package does after wiping the table in @BeforeEach.
@SpringBootTest
@TestPropertySource(properties = { "hugo.admin.email=startup-admin@hugo.local", "hugo.admin.password=startup-pw-1" })
@DirtiesContext
class AdminBootstrapStartupTest {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private JdbcClient jdbc;

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	@Test
	@DisplayName("AC3: a HUGO_ADMIN_EMAIL / HUGO_ADMIN_PASSWORD induláskor, @Value-n keresztül hozza létre az admint")
	void applicationStartupCreatesAdminFromProperties() throws Exception {
		String email = jdbc.sql("SELECT email FROM admin_account").query(String.class).single();
		assertThat(email).isEqualTo("startup-admin@hugo.local");

		MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
		mvc.perform(post("/api/auth/admin/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"startup-admin@hugo.local\",\"password\":\"startup-pw-1\"}"))
				.andExpect(status().isOk());
	}

}
