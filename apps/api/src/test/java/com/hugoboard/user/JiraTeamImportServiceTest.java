package com.hugoboard.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@SpringBootTest(properties = {
		"hugo.jira.base-url=http://127.0.0.1:1",
		"hugo.jira.email=dev@hugo.local",
		"hugo.jira.api-token=secret-token",
		"hugo.jira.board-id=535"
})
class JiraTeamImportServiceTest {

	private static final String BASE_URL = "http://127.0.0.1:1";

	private static final String SPRINT_URL = BASE_URL + "/rest/agile/1.0/board/535/sprint?state=active";

	@Autowired
	private JiraTeamImportService importService;

	@Autowired
	private RestClient.Builder jiraRestClientBuilder;

	@Autowired
	private JdbcClient jdbc;

	private MockRestServiceServer server;

	@BeforeEach
	void setUp() {
		jdbc.sql("DELETE FROM team_member").update();
		server = MockRestServiceServer.bindTo(jiraRestClientBuilder).build();
	}

	private void expectActiveSprint(long sprintId) {
		server.expect(requestTo(SPRINT_URL))
				.andRespond(withSuccess(
						"{\"values\":[{\"id\":" + sprintId + ",\"state\":\"active\"}]}",
						MediaType.APPLICATION_JSON));
	}

	private void expectIssuePage(long sprintId, int startAt, String body) {
		server.expect(requestTo(BASE_URL + "/rest/agile/1.0/sprint/" + sprintId
				+ "/issue?startAt=" + startAt + "&maxResults=50&fields=assignee"))
				.andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
	}

	private List<Map<String, Object>> teamMembers() {
		return jdbc.sql("SELECT * FROM team_member ORDER BY name").query().listOfRows();
	}

	@Test
	@DisplayName("AC1: 3 különböző assignee -> 3 csapattag jön létre a teljes adatukkal")
	void importsThreeDistinctAssignees() {
		expectActiveSprint(42);
		expectIssuePage(42, 0, """
				{"startAt":0,"maxResults":50,"total":3,"issues":[
				  {"fields":{"assignee":{"accountId":"a1","displayName":"Teszt Elek","emailAddress":"elek@hugo.local","avatarUrls":{"48x48":"https://avatar/a1.png"},"active":true}}},
				  {"fields":{"assignee":{"accountId":"a2","displayName":"Minta Anna","emailAddress":"anna@hugo.local","avatarUrls":{"48x48":"https://avatar/a2.png"},"active":true}}},
				  {"fields":{"assignee":{"accountId":"a3","displayName":"Próba Péter","emailAddress":"peter@hugo.local","avatarUrls":{"48x48":"https://avatar/a3.png"},"active":true}}}
				]}
				""");

		importService.importIfNeeded();

		List<Map<String, Object>> members = teamMembers();
		assertThat(members).hasSize(3);
		Map<String, Object> elek = members.stream()
				.filter(m -> "Teszt Elek".equals(m.get("name"))).findFirst().orElseThrow();
		assertThat(elek.get("jira_account_id")).isEqualTo("a1");
		assertThat(elek.get("avatar_url")).isEqualTo("https://avatar/a1.png");
		assertThat(elek.get("email")).isEqualTo("elek@hugo.local");
		assertThat(elek.get("imported_at")).isNotNull();
		server.verify();
	}

	@Test
	@DisplayName("AC2: ugyanaz a felhasználó több feladaton is -> egyszer szerepel")
	void deduplicatesSameAssignee() {
		expectActiveSprint(42);
		expectIssuePage(42, 0, """
				{"startAt":0,"maxResults":50,"total":2,"issues":[
				  {"fields":{"assignee":{"accountId":"a1","displayName":"Teszt Elek","active":true}}},
				  {"fields":{"assignee":{"accountId":"a1","displayName":"Teszt Elek","active":true}}}
				]}
				""");

		importService.importIfNeeded();

		assertThat(teamMembers()).hasSize(1);
	}

	@Test
	@DisplayName("AC3: assignee nélküli feladat nem hoz létre csapattagot")
	void skipsIssuesWithoutAssignee() {
		expectActiveSprint(42);
		expectIssuePage(42, 0, """
				{"startAt":0,"maxResults":50,"total":2,"issues":[
				  {"fields":{"assignee":null}},
				  {"fields":{"assignee":{"accountId":"a1","displayName":"Teszt Elek","active":true}}}
				]}
				""");

		importService.importIfNeeded();

		assertThat(teamMembers()).hasSize(1);
	}

	@Test
	@DisplayName("AC4: a feladatok több oldalon jönnek -> minden oldal assignee-i bekerülnek")
	void importsAssigneesFromEveryPage() {
		expectActiveSprint(42);
		expectIssuePage(42, 0, """
				{"startAt":0,"maxResults":50,"total":2,"issues":[
				  {"fields":{"assignee":{"accountId":"a1","displayName":"Elso","active":true}}}
				]}
				""");
		expectIssuePage(42, 1, """
				{"startAt":1,"maxResults":50,"total":2,"issues":[
				  {"fields":{"assignee":{"accountId":"a2","displayName":"Masodik","active":true}}}
				]}
				""");

		importService.importIfNeeded();

		assertThat(teamMembers()).extracting(m -> m.get("name")).containsExactlyInAnyOrder("Elso", "Masodik");
		server.verify();
	}

	@Test
	@DisplayName("AC5: hiányzó e-mail -> a csapattag üres e-maillel jön létre")
	void createsMemberWithEmptyEmailWhenMissing() {
		expectActiveSprint(42);
		expectIssuePage(42, 0, """
				{"startAt":0,"maxResults":50,"total":1,"issues":[
				  {"fields":{"assignee":{"accountId":"a1","displayName":"Teszt Elek","active":true}}}
				]}
				""");

		importService.importIfNeeded();

		assertThat(teamMembers().get(0).get("email")).isEqualTo("");
	}

	@Test
	@DisplayName("AC6: inaktív Jira-fiók -> active = 0 értékkel jön létre a csapattag")
	void importsInactiveAssigneeAsInactiveMember() {
		expectActiveSprint(42);
		expectIssuePage(42, 0, """
				{"startAt":0,"maxResults":50,"total":1,"issues":[
				  {"fields":{"assignee":{"accountId":"a1","displayName":"Inaktív Ilona","active":false}}}
				]}
				""");

		importService.importIfNeeded();

		Map<String, Object> member = teamMembers().get(0);
		assertThat(((Number) member.get("active")).intValue()).isZero();
	}

	@Test
	@DisplayName("AC7: nem üres tábla -> nincs Jira-hívás, a csapattagok változatlanok")
	void skipsImportWhenTableNotEmpty() {
		jdbc.sql("INSERT INTO team_member (name) VALUES ('Meglevo Tag')").update();

		assertThatCode(() -> importService.importIfNeeded()).doesNotThrowAnyException();

		assertThat(teamMembers()).hasSize(1);
		assertThat(teamMembers().get(0).get("name")).isEqualTo("Meglevo Tag");
		server.verify();
	}

	@Test
	@DisplayName("AC8: a Jira hitelesítési hibát ad (401) -> az app elindul, a tábla üres marad")
	void leavesTableEmptyOnUnauthorized() {
		server.expect(requestTo(SPRINT_URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

		assertThatCode(() -> importService.importIfNeeded()).doesNotThrowAnyException();

		assertThat(teamMembers()).isEmpty();
	}

	@Test
	@DisplayName("AC8: a Jira szerverhibát ad (500) -> az app elindul, a tábla üres marad")
	void leavesTableEmptyOnServerError() {
		server.expect(requestTo(SPRINT_URL)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

		assertThatCode(() -> importService.importIfNeeded()).doesNotThrowAnyException();

		assertThat(teamMembers()).isEmpty();
	}

	@Test
	@DisplayName("AC9: hiba a lapozás közben (a második oldalnál) -> egyetlen tag sem mentődik")
	void savesNothingWhenPagingFailsMidway() {
		expectActiveSprint(42);
		expectIssuePage(42, 0, """
				{"startAt":0,"maxResults":50,"total":2,"issues":[
				  {"fields":{"assignee":{"accountId":"a1","displayName":"Elso","active":true}}}
				]}
				""");
		server.expect(requestTo(BASE_URL + "/rest/agile/1.0/sprint/42/issue?startAt=1&maxResults=50&fields=assignee"))
				.andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

		assertThatCode(() -> importService.importIfNeeded()).doesNotThrowAnyException();

		assertThat(teamMembers()).isEmpty();
	}

	@Test
	@DisplayName("AC10: a beállított tábla nem létezik (404) -> a tábla üres marad")
	void leavesTableEmptyWhenBoardNotFound() {
		server.expect(requestTo(SPRINT_URL)).andRespond(withStatus(HttpStatus.NOT_FOUND));

		assertThatCode(() -> importService.importIfNeeded()).doesNotThrowAnyException();

		assertThat(teamMembers()).isEmpty();
	}

	@Test
	@DisplayName("AC10: nincs aktív sprint -> a tábla üres marad")
	void leavesTableEmptyWhenNoActiveSprint() {
		server.expect(requestTo(SPRINT_URL))
				.andRespond(withSuccess("{\"values\":[]}", MediaType.APPLICATION_JSON));

		assertThatCode(() -> importService.importIfNeeded()).doesNotThrowAnyException();

		assertThat(teamMembers()).isEmpty();
	}

	@Test
	@DisplayName("AC11: a Jira nem válaszol (I/O hiba) -> a tábla üres marad")
	void leavesTableEmptyOnTransportFailure() {
		server.expect(requestTo(SPRINT_URL)).andRespond(request -> {
			throw new java.net.SocketTimeoutException("simulated timeout");
		});

		assertThatCode(() -> importService.importIfNeeded()).doesNotThrowAnyException();

		assertThat(teamMembers()).isEmpty();
	}

	@Nested
	@DisplayName("AC14: részlegesen megadott Jira-beállítás")
	@SpringBootTest(properties = {
			"hugo.jira.base-url=http://127.0.0.1:1",
			"hugo.jira.email=dev@hugo.local",
			"hugo.jira.api-token=",
			"hugo.jira.board-id=535"
	})
	class PartiallyConfigured {

		@Autowired
		private JiraTeamImportService importService;

		@Autowired
		private JdbcClient jdbc;

		@BeforeEach
		void clear() {
			jdbc.sql("DELETE FROM team_member").update();
		}

		@Test
		@DisplayName("AC14: hiányzó token esetén nincs Jira-hívás, a tábla üres marad")
		void skipsImportWhenOnlyPartiallyConfigured() {
			assertThatCode(() -> importService.importIfNeeded()).doesNotThrowAnyException();

			assertThat(jdbc.sql("SELECT COUNT(*) FROM team_member").query(Long.class).single()).isZero();
		}

	}

}
