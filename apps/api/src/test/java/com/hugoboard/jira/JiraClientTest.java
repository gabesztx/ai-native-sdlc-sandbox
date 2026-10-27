package com.hugoboard.jira;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.SocketTimeoutException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
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

}
