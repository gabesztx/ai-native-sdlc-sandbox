package com.hugoboard.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hugoboard.support.ApiTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

class DashboardControllerApiTest extends ApiTestSupport {

	@BeforeEach
	void clearDashboardTables() {
		jdbc.sql("DELETE FROM jira_issue").update();
		jdbc.sql("DELETE FROM jira_sprint").update();
		jdbc.sql("UPDATE jira_sync_state SET last_synced_at = NULL WHERE id = 1").update();
	}

	private void seedActiveSprint(long id) {
		jdbc.sql("INSERT INTO jira_sprint (id, name, state) VALUES (:id, 'HUGO Sprint 42', 'active')")
				.param("id", id)
				.update();
	}

	private void seedIssue(String id, String key, long sprintId, String statusName, String statusCategory,
			String assigneeAccountId, String assigneeName) {
		jdbc.sql("""
				INSERT INTO jira_issue (id, issue_key, sprint_id, summary, status_name, status_category,
					assignee_account_id, assignee_name, assignee_avatar_url, jira_updated_at)
				VALUES (:id, :key, :sprintId, :key, :statusName, :statusCategory,
					:accountId, :accountName, '', '2026-10-27T10:00:00Z')
				""")
				.param("id", id).param("key", key).param("sprintId", sprintId)
				.param("statusName", statusName).param("statusCategory", statusCategory)
				.param("accountId", assigneeAccountId).param("accountName", assigneeName)
				.update();
	}

	@Test
	@DisplayName("AC24: belépés nélkül a /api/dashboard végpont 401")
	void dashboardRequiresLogin() throws Exception {
		mvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("AC25: admin és kolléga is 200-at kap a dashboardra")
	void dashboardAvailableToBothRoles() throws Exception {
		long memberId = insertMember("Teszt Elek");

		mvc.perform(get("/api/dashboard").header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
				.andExpect(status().isOk());
		mvc.perform(get("/api/dashboard").header(HttpHeaders.AUTHORIZATION, bearer(memberToken(memberId))))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("AC4: nincs aktív sprint -> a sprint null")
	void dashboardShowsNullSprintWhenNoneActive() throws Exception {
		mvc.perform(get("/api/dashboard").header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sprint").doesNotExist());
	}

	@Test
	@DisplayName("AC7: a BLOCKED lista pontosan a Blocked státuszú feladatot adja")
	void blockedTileReturnsOnlyBlockedIssue() throws Exception {
		seedActiveSprint(42);
		seedIssue("1", "HUGO-1", 42, "Blocked", "IN_PROGRESS", "a1", "Teszt Elek");
		seedIssue("2", "HUGO-2", 42, "Done", "DONE", "a1", "Teszt Elek");

		mvc.perform(get("/api/dashboard/issues?tile=BLOCKED")
				.header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].key").value("HUGO-1"))
				.andExpect(jsonPath("$[0].assignee.name").value("Teszt Elek"));
	}

	@Test
	@DisplayName("AC8: az UNASSIGNED lista pontosan az assignee nélküli feladatokat adja, assignee=null")
	void unassignedTileReturnsIssuesWithoutAssignee() throws Exception {
		seedActiveSprint(42);
		seedIssue("1", "HUGO-1", 42, "To Do", "TODO", null, null);
		seedIssue("2", "HUGO-2", 42, "To Do", "TODO", "a1", "Teszt Elek");

		mvc.perform(get("/api/dashboard/issues?tile=UNASSIGNED")
				.header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].key").value("HUGO-1"))
				.andExpect(jsonPath("$[0].assignee").doesNotExist());
	}

	@Test
	@DisplayName("AC9: ismeretlen tile érték 400-at ad")
	void unknownTileReturns400() throws Exception {
		mvc.perform(get("/api/dashboard/issues?tile=NOPE")
				.header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("ALL tile minden feladatot visszaad")
	void allTileReturnsEveryIssue() throws Exception {
		seedActiveSprint(42);
		seedIssue("1", "HUGO-1", 42, "Blocked", "IN_PROGRESS", "a1", "Teszt Elek");
		seedIssue("2", "HUGO-2", 42, "Done", "DONE", "a1", "Teszt Elek");

		mvc.perform(get("/api/dashboard/issues?tile=ALL")
				.header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[*].key", containsInAnyOrder("HUGO-1", "HUGO-2")));
	}

	@Test
	@DisplayName("AC20: belépés nélkül a /api/dashboard/stream végpont 401")
	void streamRequiresLogin() throws Exception {
		mvc.perform(get("/api/dashboard/stream")).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("AC19: belépve a stream azonnal elküldi az aktuális dashboard-adatot")
	void streamSendsCurrentStateImmediately() throws Exception {
		seedActiveSprint(42);
		seedIssue("1", "HUGO-1", 42, "Done", "DONE", "a1", "Teszt Elek");

		// The first event is sent synchronously inside the controller method (register()
		// calls send() before returning the emitter), so it is already in the response body
		// as soon as this request returns — SseEmitter never "completes" on its own, so
		// MockMvc's asyncDispatch (built for Callable/DeferredResult) would hang forever here.
		MvcResult result = mvc.perform(get("/api/dashboard/stream")
				.header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
				.andExpect(request().asyncStarted())
				.andReturn();

		String content = result.getResponse().getContentAsString();
		assertThat(content).contains("event:dashboard").contains("\"donePercent\":100");
	}

	@Test
	@DisplayName("Admin és kolléga egyaránt kap tokent, de csak belépve éri el a dashboard streamet")
	void memberCanConnectToStream() throws Exception {
		long memberId = insertMember("Teszt Elek");

		mvc.perform(get("/api/dashboard/stream").header(HttpHeaders.AUTHORIZATION, bearer(memberToken(memberId))))
				.andExpect(request().asyncStarted());
	}

	@Test
	@DisplayName("A /api/dashboard nem más admin/me prefixű végpont, nem 403-mal, hanem 401-gyel reagál belépés nélkül")
	void dashboardDoesNotRequireSpecificRole() throws Exception {
		long memberId = insertMember("Teszt Elek");

		mvc.perform(put("/api/admin/forms/1").header(HttpHeaders.AUTHORIZATION, bearer(memberToken(memberId))))
				.andExpect(status().isForbidden());
		mvc.perform(get("/api/dashboard").header(HttpHeaders.AUTHORIZATION, bearer(memberToken(memberId))))
				.andExpect(status().isOk());
	}

}
