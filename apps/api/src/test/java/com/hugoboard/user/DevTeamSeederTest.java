package com.hugoboard.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.between;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Each nested class gets its own ApplicationContext (and therefore its own fresh SQLite
 * file), so the dev-profile ApplicationRunners (DevTeamSeeder, JiraTeamImportRunner) are
 * observed exactly once, right after startup, under a different Jira configuration.
 */
class DevTeamSeederTest {

	private static final List<String> DEMO_NAMES = List.of(
			"Teszt Elek", "Minta Anna", "Próba Péter", "Kitalált Kata", "Példa Pál");

	@Nested
	@SpringBootTest
	@ActiveProfiles("dev")
	@DisplayName("AC12: dev profil, nincs Jira-beállítás")
	class WithoutJiraConfigured {

		@Autowired
		private JdbcClient jdbc;

		@Test
		@DisplayName("AC12: a kitalált csapattagok töltődnek be, a Jira nem kap hívást")
		void seedsDemoTeamWhenJiraNotConfigured() {
			List<String> names = jdbc.sql("SELECT name FROM team_member ORDER BY name")
					.query(String.class)
					.list();

			assertThat(names).containsExactlyInAnyOrderElementsOf(DEMO_NAMES);
		}

	}

	@Nested
	@SpringBootTest(properties = {
			"hugo.jira.base-url=http://127.0.0.1:1",
			"hugo.jira.email=dev@hugo.local",
			"hugo.jira.api-token=secret-token",
			"hugo.jira.board-id=535"
	})
	@ActiveProfiles("dev")
	@Import(WithJiraConfigured.PresetJiraServerConfig.class)
	@DisplayName("AC13: dev profil, van Jira-beállítás")
	class WithJiraConfigured {

		@Autowired
		private JdbcClient jdbc;

		@Test
		@DisplayName("AC13: az import fut, kitalált csapattag nem jön létre")
		void importsRealTeamAndSkipsDemoNames() {
			List<String> names = jdbc.sql("SELECT name FROM team_member ORDER BY name")
					.query(String.class)
					.list();

			assertThat(names).containsExactlyInAnyOrder("Valodi Vera");
			assertThat(names).noneMatch(DEMO_NAMES::contains);
		}

		/**
		 * A @Bean (not @Lazy) is created during context refresh, which happens before
		 * Spring Boot invokes the ApplicationRunners — so the mock responses are already
		 * registered on the shared builder by the time JiraTeamImportRunner and
		 * DashboardStartupRunner (both ApplicationRunners, order unspecified between them)
		 * call out. ignoreExpectOrder + ExpectedCount ranges make the setup independent of
		 * which of the two runs first.
		 */
		@TestConfiguration
		static class PresetJiraServerConfig {

			@Bean
			MockRestServiceServer presetJiraServer(RestClient.Builder jiraRestClientBuilder) {
				MockRestServiceServer server = MockRestServiceServer.bindTo(jiraRestClientBuilder)
						.ignoreExpectOrder(true)
						.build();
				server.expect(between(1, 2),
						requestTo("http://127.0.0.1:1/rest/agile/1.0/board/535/sprint?state=active"))
						.andRespond(withSuccess(
								"{\"values\":[{\"id\":42,\"name\":\"HUGO Sprint 1\",\"state\":\"active\"}]}",
								MediaType.APPLICATION_JSON));
				server.expect(once(), requestTo(
						"http://127.0.0.1:1/rest/agile/1.0/sprint/42/issue?startAt=0&maxResults=50&fields=assignee"))
						.andRespond(withSuccess("""
								{"startAt":0,"maxResults":50,"total":1,"issues":[
								  {"fields":{"assignee":{"accountId":"a1","displayName":"Valodi Vera","active":true}}}
								]}
								""", MediaType.APPLICATION_JSON));
				server.expect(once(), requestTo("http://127.0.0.1:1/rest/agile/1.0/sprint/42/issue?startAt=0"
						+ "&maxResults=50&fields=summary%2Cstatus%2Cassignee%2Cupdated"))
						.andRespond(withSuccess(
								"{\"startAt\":0,\"maxResults\":50,\"total\":0,\"issues\":[]}",
								MediaType.APPLICATION_JSON));
				return server;
			}

		}

	}

}
