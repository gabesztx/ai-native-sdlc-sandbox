package com.hugoboard.jira;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;

/**
 * No Spring context: JiraClient is exercised directly against a RestClient.Builder bound
 * to MockRestServiceServer, exactly as a test would bind the singleton JiraConfig bean.
 */
class JiraClientTest {

	private static final String BASE_URL = "https://hugo.atlassian.net";

	private MockRestServiceServer server;

	private JiraClient newClient() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		JiraProperties properties = new JiraProperties(BASE_URL, "dev@hugo.local", "secret-token", "535");
		return new JiraClient(builder, properties);
	}

	@Test
	void findsActiveSprintId() {
		JiraClient client = newClient();
		server.expect(requestTo(BASE_URL + "/rest/agile/1.0/board/535/sprint?state=active"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess("""
						{"values":[{"id":42,"state":"active"}]}
						""", MediaType.APPLICATION_JSON));

		long sprintId = client.findActiveSprintId();

		assertThat(sprintId).isEqualTo(42L);
		server.verify();
	}

	@Test
	@DisplayName("AC10: nincs aktív sprint -> JiraImportException")
	void throwsWhenNoActiveSprint() {
		JiraClient client = newClient();
		server.expect(requestTo(BASE_URL + "/rest/agile/1.0/board/535/sprint?state=active"))
				.andRespond(withSuccess("{\"values\":[]}", MediaType.APPLICATION_JSON));

		assertThatThrownBy(client::findActiveSprintId).isInstanceOf(JiraImportException.class);
	}

	@Test
	@DisplayName("AC10: a tábla nem található (404) -> JiraImportException")
	void throwsWhenBoardNotFound() {
		JiraClient client = newClient();
		server.expect(requestTo(BASE_URL + "/rest/agile/1.0/board/535/sprint?state=active"))
				.andRespond(withStatus(HttpStatus.NOT_FOUND));

		assertThatThrownBy(client::findActiveSprintId).isInstanceOf(JiraImportException.class);
	}

	@Test
	@DisplayName("AC8: hitelesítési hiba (401) -> JiraImportException, a token nem szerepel benne")
	void throwsOnUnauthorized() {
		JiraClient client = newClient();
		server.expect(requestTo(BASE_URL + "/rest/agile/1.0/board/535/sprint?state=active"))
				.andRespond(withStatus(HttpStatus.UNAUTHORIZED));

		assertThatThrownBy(client::findActiveSprintId)
				.isInstanceOf(JiraImportException.class)
				.hasMessageNotContaining("secret-token");
	}

	@Test
	@DisplayName("AC8: szerverhiba (500) -> JiraImportException")
	void throwsOnServerError() {
		JiraClient client = newClient();
		server.expect(requestTo(BASE_URL + "/rest/agile/1.0/board/535/sprint?state=active"))
				.andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

		assertThatThrownBy(client::findActiveSprintId).isInstanceOf(JiraImportException.class);
	}

	@Test
	@DisplayName("AC11: a Jira nem válaszol (I/O hiba) -> JiraImportException")
	void throwsOnTransportFailure() {
		JiraClient client = newClient();
		server.expect(requestTo(BASE_URL + "/rest/agile/1.0/board/535/sprint?state=active"))
				.andRespond(request -> {
					throw new SocketTimeoutException("simulated timeout");
				});

		assertThatThrownBy(client::findActiveSprintId).isInstanceOf(JiraImportException.class);
	}

	@Test
	@DisplayName("AC3: assignee nélküli feladat nem kerül be az eredménybe")
	void skipsIssuesWithoutAssignee() {
		JiraClient client = newClient();
		server.expect(requestTo(BASE_URL
				+ "/rest/agile/1.0/sprint/42/issue?startAt=0&maxResults=50&fields=assignee"))
				.andRespond(withSuccess("""
						{"startAt":0,"maxResults":50,"total":2,"issues":[
						  {"fields":{"assignee":null}},
						  {"fields":{"assignee":{"accountId":"a1","displayName":"Teszt Elek",
						    "emailAddress":"elek@hugo.local",
						    "avatarUrls":{"48x48":"https://avatar/a1.png"},"active":true}}}
						]}
						""", MediaType.APPLICATION_JSON));

		List<JiraUser> assignees = client.fetchAssignees(42);

		assertThat(assignees).hasSize(1);
		assertThat(assignees.get(0).accountId()).isEqualTo("a1");
		assertThat(assignees.get(0).avatarUrl()).isEqualTo("https://avatar/a1.png");
	}

	@Test
	@DisplayName("AC2: ugyanaz az assignee több feladaton is szerepelhet a nyers listában (a dedup a service-ben történik)")
	void includesSameAssigneeOncePerIssue() {
		JiraClient client = newClient();
		server.expect(requestTo(BASE_URL
				+ "/rest/agile/1.0/sprint/42/issue?startAt=0&maxResults=50&fields=assignee"))
				.andRespond(withSuccess("""
						{"startAt":0,"maxResults":50,"total":2,"issues":[
						  {"fields":{"assignee":{"accountId":"a1","displayName":"Teszt Elek"}}},
						  {"fields":{"assignee":{"accountId":"a1","displayName":"Teszt Elek"}}}
						]}
						""", MediaType.APPLICATION_JSON));

		List<JiraUser> assignees = client.fetchAssignees(42);

		assertThat(assignees).hasSize(2);
	}

	@Test
	@DisplayName("AC4: a lapozás minden oldalt lekér, amíg a total el nem éri")
	void paginatesUntilAllIssuesFetched() {
		JiraClient client = newClient();
		server.expect(requestTo(BASE_URL
				+ "/rest/agile/1.0/sprint/42/issue?startAt=0&maxResults=50&fields=assignee"))
				.andRespond(withSuccess("""
						{"startAt":0,"maxResults":50,"total":2,"issues":[
						  {"fields":{"assignee":{"accountId":"a1","displayName":"Elso"}}}
						]}
						""", MediaType.APPLICATION_JSON));
		server.expect(requestTo(BASE_URL
				+ "/rest/agile/1.0/sprint/42/issue?startAt=1&maxResults=50&fields=assignee"))
				.andRespond(withSuccess("""
						{"startAt":1,"maxResults":50,"total":2,"issues":[
						  {"fields":{"assignee":{"accountId":"a2","displayName":"Masodik"}}}
						]}
						""", MediaType.APPLICATION_JSON));

		List<JiraUser> assignees = client.fetchAssignees(42);

		assertThat(assignees).extracting(JiraUser::accountId).containsExactly("a1", "a2");
		server.verify();
	}

	@Test
	@DisplayName("A teljes sprint (név, állapot, dátumok) lekérhető, nem csak az azonosítója")
	void findsFullActiveSprint() {
		JiraClient client = newClient();
		server.expect(requestTo(BASE_URL + "/rest/agile/1.0/board/535/sprint?state=active"))
				.andRespond(withSuccess("""
						{"values":[{"id":42,"name":"HUGO Sprint 42","state":"active",
						  "startDate":"2026-10-20T08:00:00.000Z","endDate":"2026-11-03T16:00:00.000Z"}]}
						""", MediaType.APPLICATION_JSON));

		JiraSprint sprint = client.findActiveSprint();

		assertThat(sprint.id()).isEqualTo(42L);
		assertThat(sprint.name()).isEqualTo("HUGO Sprint 42");
		assertThat(sprint.startDate()).isEqualTo("2026-10-20T08:00:00.000Z");
		assertThat(sprint.endDate()).isEqualTo("2026-11-03T16:00:00.000Z");
	}

	@Test
	@DisplayName("AC10: nincs aktív sprint -> JiraNoActiveSprintException, nem általános JiraImportException")
	void noActiveSprintThrowsDedicatedException() {
		JiraClient client = newClient();
		server.expect(requestTo(BASE_URL + "/rest/agile/1.0/board/535/sprint?state=active"))
				.andRespond(withSuccess("{\"values\":[]}", MediaType.APPLICATION_JSON));

		assertThatThrownBy(client::findActiveSprint).isInstanceOf(JiraNoActiveSprintException.class);
	}

	@Test
	@DisplayName("A teljes feladatlekérés (summary/status/assignee/updated mezőkkel) minden adatot megad")
	void fetchesFullFieldIssues() {
		JiraClient client = newClient();
		server.expect(requestTo(BASE_URL + "/rest/agile/1.0/sprint/42/issue?startAt=0&maxResults=50"
				+ "&fields=summary%2Cstatus%2Cassignee%2Cupdated"))
				.andRespond(withSuccess("""
						{"startAt":0,"maxResults":50,"total":1,"issues":[
						  {"id":"1001","key":"HUGO-1","fields":{"summary":"Elso feladat",
						    "status":{"name":"Blocked","statusCategory":{"key":"indeterminate"}},
						    "assignee":{"accountId":"a1","displayName":"Teszt Elek"},
						    "updated":"2026-10-27T10:00:00.000Z"}}
						]}
						""", MediaType.APPLICATION_JSON));

		List<JiraIssue> issues = client.fetchActiveSprintIssues(42);

		assertThat(issues).hasSize(1);
		JiraIssue issue = issues.get(0);
		assertThat(issue.id()).isEqualTo("1001");
		assertThat(issue.key()).isEqualTo("HUGO-1");
		assertThat(issue.summary()).isEqualTo("Elso feladat");
		assertThat(issue.status().name()).isEqualTo("Blocked");
		assertThat(issue.status().statusCategory().key()).isEqualTo("indeterminate");
		assertThat(issue.updated()).isEqualTo("2026-10-27T10:00:00.000Z");
	}

	@Test
	@DisplayName("A kulcs-lista lekérés csak a feladatkulcsokat kéri le (olcsó törlésellenőrzéshez)")
	void fetchesIssueKeysOnly() {
		JiraClient client = newClient();
		server.expect(requestTo(BASE_URL + "/rest/agile/1.0/sprint/42/issue?startAt=0&maxResults=50&fields=key"))
				.andRespond(withSuccess("""
						{"startAt":0,"maxResults":50,"total":2,"issues":[
						  {"id":"1","key":"HUGO-1","fields":{}},
						  {"id":"2","key":"HUGO-2","fields":{}}
						]}
						""", MediaType.APPLICATION_JSON));

		List<String> keys = client.fetchActiveSprintIssueKeys(42);

		assertThat(keys).containsExactly("HUGO-1", "HUGO-2");
	}

	@Test
	@DisplayName("AC15: a delta-lekérdezés JQL-ben szűr a legutóbbi szinkron ideje óta módosultakra")
	void fetchesOnlyIssuesUpdatedSince() {
		JiraClient client = newClient();
		server.expect(requestTo(BASE_URL + "/rest/agile/1.0/sprint/42/issue?startAt=0&maxResults=50"
				+ "&fields=summary,status,assignee,updated&jql=updated%20%3E%3D%20%222026-10-27T10%3A00%3A00Z%22"))
				.andRespond(withSuccess("""
						{"startAt":0,"maxResults":50,"total":1,"issues":[
						  {"id":"1001","key":"HUGO-1","fields":{"summary":"Valtozott",
						    "status":{"name":"Done","statusCategory":{"key":"done"}},
						    "updated":"2026-10-27T10:05:00.000Z"}}
						]}
						""", MediaType.APPLICATION_JSON));

		List<JiraIssue> issues = client.fetchIssuesUpdatedSince(42, "2026-10-27T10:00:00Z");

		assertThat(issues).extracting(JiraIssue::key).containsExactly("HUGO-1");
	}

	@Test
	@DisplayName("Ha a feladat még létezik a Jirában (más sprintben vagy a backlogban), issueExists igazzal tér vissza")
	void issueExistsReturnsTrueWhenFound() {
		JiraClient client = newClient();
		server.expect(requestTo(BASE_URL + "/rest/api/3/issue/HUGO-1?fields=key"))
				.andRespond(withSuccess("{\"id\":\"1001\",\"key\":\"HUGO-1\"}", MediaType.APPLICATION_JSON));

		assertThat(client.issueExists("HUGO-1")).isTrue();
	}

	@Test
	@DisplayName("AC14: ha a feladatot törölték a Jirában, issueExists hamissal tér vissza")
	void issueExistsReturnsFalseWhenDeleted() {
		JiraClient client = newClient();
		server.expect(requestTo(BASE_URL + "/rest/api/3/issue/HUGO-1?fields=key"))
				.andRespond(withStatus(HttpStatus.NOT_FOUND));

		assertThat(client.issueExists("HUGO-1")).isFalse();
	}

	@Test
	@DisplayName("AC17: 429 válasz esetén JiraRateLimitException, a Retry-After fejléccel")
	void rateLimitCarriesRetryAfter() {
		JiraClient client = newClient();
		server.expect(requestTo(BASE_URL + "/rest/agile/1.0/board/535/sprint?state=active"))
				.andRespond(MockRestResponseCreators.withStatus(HttpStatus.TOO_MANY_REQUESTS)
						.header(HttpHeaders.RETRY_AFTER, "120"));

		assertThatThrownBy(client::findActiveSprint)
				.isInstanceOf(JiraRateLimitException.class)
				.satisfies(e -> assertThat(((JiraRateLimitException) e).retryAfter()).isEqualTo(Duration.ofSeconds(120)));
	}

}
