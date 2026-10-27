package com.hugoboard.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.hugoboard.support.TestClockConfig;
import com.hugoboard.support.MutableClock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
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
@Import(TestClockConfig.class)
class DashboardSyncServiceTest {

	private static final String BASE_URL = "http://127.0.0.1:1";

	private static final String SPRINT_URL = BASE_URL + "/rest/agile/1.0/board/535/sprint?state=active";

	@Autowired
	private DashboardSyncService syncService;

	@Autowired
	private DashboardService dashboardService;

	@Autowired
	private RestClient.Builder jiraRestClientBuilder;

	@Autowired
	private JdbcClient jdbc;

	@Autowired
	private MutableClock clock;

	private MockRestServiceServer server;

	@BeforeEach
	void setUp() {
		jdbc.sql("DELETE FROM jira_issue").update();
		jdbc.sql("DELETE FROM jira_sprint").update();
		jdbc.sql("UPDATE jira_sync_state SET last_synced_at = NULL WHERE id = 1").update();
		clock.reset();
		syncService.resetBackOff();
		server = MockRestServiceServer.bindTo(jiraRestClientBuilder).ignoreExpectOrder(true).build();
	}

	private void expectActiveSprint(long id, String name, String startDate, String endDate) {
		String startJson = startDate == null ? "null" : "\"" + startDate + "\"";
		String endJson = endDate == null ? "null" : "\"" + endDate + "\"";
		server.expect(requestTo(SPRINT_URL))
				.andRespond(withSuccess(
						"{\"values\":[{\"id\":%d,\"name\":\"%s\",\"state\":\"active\",\"startDate\":%s,\"endDate\":%s}]}"
								.formatted(id, name, startJson, endJson),
						MediaType.APPLICATION_JSON));
	}

	private void expectNoActiveSprint() {
		server.expect(requestTo(SPRINT_URL)).andRespond(withSuccess("{\"values\":[]}", MediaType.APPLICATION_JSON));
	}

	private void expectFullFetch(long sprintId, String issuesJson) {
		server.expect(requestTo(BASE_URL + "/rest/agile/1.0/sprint/" + sprintId
				+ "/issue?startAt=0&maxResults=50&fields=summary%2Cstatus%2Cassignee%2Cupdated"))
				.andRespond(withSuccess(issuesJson, MediaType.APPLICATION_JSON));
	}

	private void expectDeltaFetch(long sprintId, String updatedAtIso, String issuesJson) {
		String jql = "jql=updated%20%3E%3D%20%22" + updatedAtIso.replace(":", "%3A") + "%22";
		server.expect(requestTo(BASE_URL + "/rest/agile/1.0/sprint/" + sprintId
				+ "/issue?startAt=0&maxResults=50&fields=summary,status,assignee,updated&" + jql))
				.andRespond(withSuccess(issuesJson, MediaType.APPLICATION_JSON));
	}

	private void expectKeyList(long sprintId, String keysJson) {
		server.expect(requestTo(BASE_URL + "/rest/agile/1.0/sprint/" + sprintId
				+ "/issue?startAt=0&maxResults=50&fields=key"))
				.andRespond(withSuccess(keysJson, MediaType.APPLICATION_JSON));
	}

	private void expectIssueExists(String key, boolean exists) {
		if (exists) {
			server.expect(requestTo(BASE_URL + "/rest/api/3/issue/" + key + "?fields=key"))
					.andRespond(withSuccess("{\"id\":\"1\",\"key\":\"" + key + "\"}", MediaType.APPLICATION_JSON));
		} else {
			server.expect(requestTo(BASE_URL + "/rest/api/3/issue/" + key + "?fields=key"))
					.andRespond(withStatus(HttpStatus.NOT_FOUND));
		}
	}

	private void seedSprint(long id, String name, String state, String startDate, String endDate) {
		jdbc.sql("""
				INSERT INTO jira_sprint (id, name, state, start_date, end_date)
				VALUES (:id, :name, :state, :startDate, :endDate)
				""")
				.param("id", id).param("name", name).param("state", state)
				.param("startDate", startDate).param("endDate", endDate)
				.update();
	}

	private void seedIssue(String id, String key, Long sprintId, String summary, String statusName,
			String statusCategory, String assigneeAccountId, String jiraUpdatedAt) {
		jdbc.sql("""
				INSERT INTO jira_issue (id, issue_key, sprint_id, summary, status_name, status_category,
					assignee_account_id, assignee_name, assignee_avatar_url, jira_updated_at)
				VALUES (:id, :key, :sprintId, :summary, :statusName, :statusCategory, :accountId, :accountId, '', :updatedAt)
				""")
				.param("id", id).param("key", key).param("sprintId", sprintId).param("summary", summary)
				.param("statusName", statusName).param("statusCategory", statusCategory)
				.param("accountId", assigneeAccountId).param("updatedAt", jiraUpdatedAt)
				.update();
	}

	private void seedLastSyncedAt(String timestamp) {
		jdbc.sql("UPDATE jira_sync_state SET last_synced_at = :timestamp WHERE id = 1")
				.param("timestamp", timestamp)
				.update();
	}

	private String lastSyncedAt() {
		return jdbc.sql("SELECT last_synced_at FROM jira_sync_state WHERE id = 1")
				.query((rs, rowNum) -> rs.getString("last_synced_at"))
				.list()
				.get(0);
	}

	private Long sprintIdOf(String issueKey) {
		return jdbc.sql("SELECT sprint_id FROM jira_issue WHERE issue_key = :key")
				.param("key", issueKey)
				.query((rs, rowNum) -> (Long) rs.getObject("sprint_id"))
				.list()
				.get(0);
	}

	@Test
	@DisplayName("AC22: üres pillanatkép és elérhető Jira esetén a kezdeti betöltés elmenti az aktív sprintet és minden feladatát")
	void initialLoadSavesActiveSprintAndAllIssues() {
		expectActiveSprint(42, "HUGO Sprint 42", "2026-10-20T08:00:00Z", "2026-11-03T16:00:00Z");
		expectFullFetch(42, """
				{"startAt":0,"maxResults":50,"total":1,"issues":[
				  {"id":"1001","key":"HUGO-1","fields":{"summary":"Elso feladat",
				    "status":{"name":"Blocked","statusCategory":{"key":"indeterminate"}},
				    "assignee":{"accountId":"a1","displayName":"Teszt Elek"},
				    "updated":"2026-10-27T10:00:00Z"}}
				]}
				""");

		syncService.syncNow();

		Dashboard dashboard = dashboardService.getDashboard();
		assertThat(dashboard.sprint()).isNotNull();
		assertThat(dashboard.sprint().name()).isEqualTo("HUGO Sprint 42");
		assertThat(dashboard.updatedAt()).isNotNull();
		assertThat(dashboard.kpis().total()).isEqualTo(1);
		assertThat(dashboard.kpis().blocked()).isEqualTo(1);
		assertThat(lastSyncedAt()).isNotNull();
	}

	@Test
	@DisplayName("AC23: ha a Jira nem elérhető induláskor, az app elindul, a pillanatkép üres marad")
	void leavesSnapshotEmptyWhenJiraUnavailable() {
		server.expect(requestTo(SPRINT_URL)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

		syncService.syncNow();

		Dashboard dashboard = dashboardService.getDashboard();
		assertThat(dashboard.sprint()).isNull();
		assertThat(dashboard.updatedAt()).isNull();
		assertThat(lastSyncedAt()).isNull();
	}

	@Test
	@DisplayName("AC1: vegyes státuszú feladatokból helyesen számolt KPI-k")
	void computesKpisForMixedStatuses() {
		expectActiveSprint(42, "HUGO Sprint 42", null, null);
		expectFullFetch(42, """
				{"startAt":0,"maxResults":50,"total":10,"issues":[
				  {"id":"1","key":"HUGO-1","fields":{"summary":"d1","status":{"name":"Done","statusCategory":{"key":"done"}},"assignee":{"accountId":"a1","displayName":"X"},"updated":"2026-10-27T10:00:00Z"}},
				  {"id":"2","key":"HUGO-2","fields":{"summary":"d2","status":{"name":"Done","statusCategory":{"key":"done"}},"assignee":{"accountId":"a1","displayName":"X"},"updated":"2026-10-27T10:00:00Z"}},
				  {"id":"3","key":"HUGO-3","fields":{"summary":"d3","status":{"name":"Done","statusCategory":{"key":"done"}},"assignee":{"accountId":"a1","displayName":"X"},"updated":"2026-10-27T10:00:00Z"}},
				  {"id":"4","key":"HUGO-4","fields":{"summary":"d4","status":{"name":"Done","statusCategory":{"key":"done"}},"assignee":{"accountId":"a1","displayName":"X"},"updated":"2026-10-27T10:00:00Z"}},
				  {"id":"5","key":"HUGO-5","fields":{"summary":"p1","status":{"name":"Blocked","statusCategory":{"key":"indeterminate"}},"assignee":{"accountId":"a1","displayName":"X"},"updated":"2026-10-27T10:00:00Z"}},
				  {"id":"6","key":"HUGO-6","fields":{"summary":"p2","status":{"name":"In Progress","statusCategory":{"key":"indeterminate"}},"assignee":{"accountId":"a1","displayName":"X"},"updated":"2026-10-27T10:00:00Z"}},
				  {"id":"7","key":"HUGO-7","fields":{"summary":"p3","status":{"name":"In Progress","statusCategory":{"key":"indeterminate"}},"assignee":{"accountId":"a1","displayName":"X"},"updated":"2026-10-27T10:00:00Z"}},
				  {"id":"8","key":"HUGO-8","fields":{"summary":"t1","status":{"name":"To Do","statusCategory":{"key":"new"}},"updated":"2026-10-27T10:00:00Z"}},
				  {"id":"9","key":"HUGO-9","fields":{"summary":"t2","status":{"name":"To Do","statusCategory":{"key":"new"}},"updated":"2026-10-27T10:00:00Z"}},
				  {"id":"10","key":"HUGO-10","fields":{"summary":"t3","status":{"name":"To Do","statusCategory":{"key":"new"}},"assignee":{"accountId":"a1","displayName":"X"},"updated":"2026-10-27T10:00:00Z"}}
				]}
				""");

		syncService.syncNow();

		Dashboard.Kpis kpis = dashboardService.getDashboard().kpis();
		assertThat(kpis.total()).isEqualTo(10);
		assertThat(kpis.donePercent()).isEqualTo(40);
		assertThat(kpis.inProgress()).isEqualTo(3);
		assertThat(kpis.blocked()).isEqualTo(1);
		assertThat(kpis.unassigned()).isEqualTo(2);
	}

	@Test
	@DisplayName("AC2: aktív sprint feladatok nélkül -> minden KPI 0, nem hiba")
	void emptySprintHasZeroKpis() {
		expectActiveSprint(42, "HUGO Sprint 42", null, null);
		expectFullFetch(42, "{\"startAt\":0,\"maxResults\":50,\"total\":0,\"issues\":[]}");

		syncService.syncNow();

		Dashboard.Kpis kpis = dashboardService.getDashboard().kpis();
		assertThat(kpis.total()).isZero();
		assertThat(kpis.donePercent()).isZero();
	}

	@Test
	@DisplayName("AC3: 3 feladatból 1 kész -> donePercent 33 (nem kerekítve 34-re)")
	void donePercentRoundsDown() {
		expectActiveSprint(42, "HUGO Sprint 42", null, null);
		expectFullFetch(42, """
				{"startAt":0,"maxResults":50,"total":3,"issues":[
				  {"id":"1","key":"HUGO-1","fields":{"summary":"d","status":{"name":"Done","statusCategory":{"key":"done"}},"updated":"2026-10-27T10:00:00Z"}},
				  {"id":"2","key":"HUGO-2","fields":{"summary":"t1","status":{"name":"To Do","statusCategory":{"key":"new"}},"updated":"2026-10-27T10:00:00Z"}},
				  {"id":"3","key":"HUGO-3","fields":{"summary":"t2","status":{"name":"To Do","statusCategory":{"key":"new"}},"updated":"2026-10-27T10:00:00Z"}}
				]}
				""");

		syncService.syncNow();

		assertThat(dashboardService.getDashboard().kpis().donePercent()).isEqualTo(33);
	}

	@Test
	@DisplayName("AC5: 14 napos sprint, 7 nap telt el -> elapsedPercent 50, daysRemaining 7")
	void progressBarHalfway() {
		String start = clock.instant().minus(Duration.ofDays(7)).toString();
		String end = clock.instant().plus(Duration.ofDays(7)).toString();
		expectActiveSprint(42, "HUGO Sprint 42", start, end);
		expectFullFetch(42, "{\"startAt\":0,\"maxResults\":50,\"total\":0,\"issues\":[]}");

		syncService.syncNow();

		Sprint sprint = dashboardService.getDashboard().sprint();
		assertThat(sprint.elapsedPercent()).isEqualTo(50);
		assertThat(sprint.daysRemaining()).isEqualTo(7);
	}

	@Test
	@DisplayName("AC6: a sprint vége már elmúlt, de még aktív -> elapsedPercent 100, daysRemaining 0")
	void progressBarClampsWhenOverdue() {
		String start = clock.instant().minus(Duration.ofDays(20)).toString();
		String end = clock.instant().minus(Duration.ofDays(5)).toString();
		expectActiveSprint(42, "HUGO Sprint 42", start, end);
		expectFullFetch(42, "{\"startAt\":0,\"maxResults\":50,\"total\":0,\"issues\":[]}");

		syncService.syncNow();

		Sprint sprint = dashboardService.getDashboard().sprint();
		assertThat(sprint.elapsedPercent()).isEqualTo(100);
		assertThat(sprint.daysRemaining()).isZero();
	}

	@Test
	@DisplayName("AC11+AC15: a delta-lekérdezés csak a módosult feladatot frissíti, és a last_synced_at halad")
	void incrementalSyncUpdatesChangedIssue() {
		seedSprint(42, "HUGO Sprint 42", "active", null, null);
		seedIssue("1", "HUGO-1", 42L, "Feladat", "To Do", "TODO", null, "2026-10-27T09:00:00Z");
		seedLastSyncedAt("2026-10-27T09:30:00Z");

		expectActiveSprint(42, "HUGO Sprint 42", null, null);
		expectDeltaFetch(42, "2026-10-27T09:30:00Z", """
				{"startAt":0,"maxResults":50,"total":1,"issues":[
				  {"id":"1","key":"HUGO-1","fields":{"summary":"Feladat",
				    "status":{"name":"Blocked","statusCategory":{"key":"indeterminate"}},
				    "updated":"2026-10-27T10:00:00Z"}}
				]}
				""");
		expectKeyList(42, "{\"startAt\":0,\"maxResults\":50,\"total\":1,\"issues\":[{\"id\":\"1\",\"key\":\"HUGO-1\",\"fields\":{}}]}");

		syncService.syncNow();

		assertThat(dashboardService.getDashboard().kpis().blocked()).isEqualTo(1);
	}

	@Test
	@DisplayName("AC12: új feladat megjelenik az aktív sprintben -> a total eggyel nő")
	void incrementalSyncAddsNewIssue() {
		seedSprint(42, "HUGO Sprint 42", "active", null, null);
		seedIssue("1", "HUGO-1", 42L, "Regi", "To Do", "TODO", null, "2026-10-27T09:00:00Z");
		seedLastSyncedAt("2026-10-27T09:30:00Z");

		expectActiveSprint(42, "HUGO Sprint 42", null, null);
		expectDeltaFetch(42, "2026-10-27T09:30:00Z", """
				{"startAt":0,"maxResults":50,"total":1,"issues":[
				  {"id":"2","key":"HUGO-2","fields":{"summary":"Uj feladat",
				    "status":{"name":"To Do","statusCategory":{"key":"new"}},
				    "updated":"2026-10-27T10:00:00Z"}}
				]}
				""");
		expectKeyList(42, """
				{"startAt":0,"maxResults":50,"total":2,"issues":[
				  {"id":"1","key":"HUGO-1","fields":{}},{"id":"2","key":"HUGO-2","fields":{}}
				]}
				""");

		syncService.syncNow();

		assertThat(dashboardService.getDashboard().kpis().total()).isEqualTo(2);
	}

	@Test
	@DisplayName("AC13: a sprintből kikerült feladat nem számít a KPI-kba, de az adata megmarad")
	void issueRemovedFromSprintIsExcludedButKept() {
		seedSprint(42, "HUGO Sprint 42", "active", null, null);
		seedIssue("1", "HUGO-1", 42L, "Marad", "To Do", "TODO", null, "2026-10-27T09:00:00Z");
		seedIssue("2", "HUGO-2", 42L, "Kikerult", "To Do", "TODO", null, "2026-10-27T09:00:00Z");
		seedLastSyncedAt("2026-10-27T09:30:00Z");

		expectActiveSprint(42, "HUGO Sprint 42", null, null);
		expectDeltaFetch(42, "2026-10-27T09:30:00Z", "{\"startAt\":0,\"maxResults\":50,\"total\":0,\"issues\":[]}");
		expectKeyList(42, "{\"startAt\":0,\"maxResults\":50,\"total\":1,\"issues\":[{\"id\":\"1\",\"key\":\"HUGO-1\",\"fields\":{}}]}");
		expectIssueExists("HUGO-2", true);

		syncService.syncNow();

		assertThat(dashboardService.getDashboard().kpis().total()).isEqualTo(1);
		assertThat(sprintIdOf("HUGO-2")).isNull();
	}

	@Test
	@DisplayName("AC14: a Jirában törölt feladat kikerül a pillanatképből")
	void deletedIssueIsRemovedFromSnapshot() {
		seedSprint(42, "HUGO Sprint 42", "active", null, null);
		seedIssue("1", "HUGO-1", 42L, "Marad", "To Do", "TODO", null, "2026-10-27T09:00:00Z");
		seedIssue("2", "HUGO-2", 42L, "Torolve", "To Do", "TODO", null, "2026-10-27T09:00:00Z");
		seedLastSyncedAt("2026-10-27T09:30:00Z");

		expectActiveSprint(42, "HUGO Sprint 42", null, null);
		expectDeltaFetch(42, "2026-10-27T09:30:00Z", "{\"startAt\":0,\"maxResults\":50,\"total\":0,\"issues\":[]}");
		expectKeyList(42, "{\"startAt\":0,\"maxResults\":50,\"total\":1,\"issues\":[{\"id\":\"1\",\"key\":\"HUGO-1\",\"fields\":{}}]}");
		expectIssueExists("HUGO-2", false);

		syncService.syncNow();

		long remaining = jdbc.sql("SELECT COUNT(*) FROM jira_issue WHERE issue_key = 'HUGO-2'")
				.query(Long.class).single();
		assertThat(remaining).isZero();
	}

	@Test
	@DisplayName("AC16: Jira-hiba esetén a pillanatkép és a last_synced_at változatlan marad")
	void jiraErrorLeavesSnapshotUnchanged() {
		seedSprint(42, "HUGO Sprint 42", "active", null, null);
		seedIssue("1", "HUGO-1", 42L, "Eredeti", "To Do", "TODO", null, "2026-10-27T09:00:00Z");
		seedLastSyncedAt("2026-10-27T09:30:00Z");

		expectActiveSprint(42, "HUGO Sprint 42", null, null);
		server.expect(requestTo(BASE_URL + "/rest/agile/1.0/sprint/42/issue?startAt=0&maxResults=50"
				+ "&fields=summary,status,assignee,updated&jql=updated%20%3E%3D%20%222026-10-27T09%3A30%3A00Z%22"))
				.andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

		syncService.syncNow();

		String summary = jdbc.sql("SELECT summary FROM jira_issue WHERE issue_key = 'HUGO-1'")
				.query(String.class).single();
		assertThat(summary).isEqualTo("Eredeti");
		assertThat(lastSyncedAt()).isEqualTo("2026-10-27T09:30:00Z");
	}

	@Test
	@DisplayName("AC17: 429 válasz után a következő lekérdezés a várakozási idő letelte előtt nem indul el")
	void rateLimitDelaysNextPoll() {
		server.expect(requestTo(SPRINT_URL))
				.andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header(HttpHeaders.RETRY_AFTER, "60"));

		syncService.syncNow();
		// Still inside the backoff window: if this made a real call, MockRestServiceServer
		// would reject it immediately (only one expectation was ever registered).
		syncService.syncNow();
		server.verify();

		// MockRestServiceServer forbids registering new expectations once real requests have
		// been dispatched, so the post-backoff phase gets its own fresh server on the same
		// builder (same pattern @BeforeEach already uses for each test).
		clock.advance(Duration.ofSeconds(61));
		server = MockRestServiceServer.bindTo(jiraRestClientBuilder).ignoreExpectOrder(true).build();
		expectNoActiveSprint();
		syncService.syncNow();
		server.verify();
	}

	@Test
	@DisplayName("AC18: régebbi updated idejű adat nem írja felül a frissebb állapotot")
	void staleUpdateDoesNotOverwriteNewerData() {
		seedSprint(42, "HUGO Sprint 42", "active", null, null);
		seedIssue("1", "HUGO-1", 42L, "Friss allapot", "Done", "DONE", null, "2026-10-27T10:05:00Z");
		seedLastSyncedAt("2026-10-27T09:30:00Z");

		expectActiveSprint(42, "HUGO Sprint 42", null, null);
		expectDeltaFetch(42, "2026-10-27T09:30:00Z", """
				{"startAt":0,"maxResults":50,"total":1,"issues":[
				  {"id":"1","key":"HUGO-1","fields":{"summary":"Regi allapot",
				    "status":{"name":"To Do","statusCategory":{"key":"new"}},
				    "updated":"2026-10-27T10:00:00Z"}}
				]}
				""");
		expectKeyList(42, "{\"startAt\":0,\"maxResults\":50,\"total\":1,\"issues\":[{\"id\":\"1\",\"key\":\"HUGO-1\",\"fields\":{}}]}");

		syncService.syncNow();

		String summary = jdbc.sql("SELECT summary FROM jira_issue WHERE issue_key = 'HUGO-1'")
				.query(String.class).single();
		assertThat(summary).isEqualTo("Friss allapot");
	}

	@Test
	@DisplayName("AC21: lezárult sprint helyett újonnan elindult sprint adatai jelennek meg")
	void sprintTransitionShowsNewSprint() {
		seedSprint(42, "Regi Sprint", "active", null, null);
		seedIssue("1", "HUGO-1", 42L, "Regi feladat", "To Do", "TODO", null, "2026-10-27T09:00:00Z");
		seedLastSyncedAt("2026-10-27T09:30:00Z");

		expectActiveSprint(99, "Uj Sprint", null, null);
		expectFullFetch(99, """
				{"startAt":0,"maxResults":50,"total":1,"issues":[
				  {"id":"2","key":"HUGO-2","fields":{"summary":"Uj feladat",
				    "status":{"name":"To Do","statusCategory":{"key":"new"}},
				    "updated":"2026-10-27T10:00:00Z"}}
				]}
				""");

		syncService.syncNow();

		Dashboard dashboard = dashboardService.getDashboard();
		assertThat(dashboard.sprint().name()).isEqualTo("Uj Sprint");
		assertThat(dashboard.kpis().total()).isEqualTo(1);
		String oldState = jdbc.sql("SELECT state FROM jira_sprint WHERE id = 42").query(String.class).single();
		assertThat(oldState).isEqualTo("closed");
	}

	@Test
	@DisplayName("Nincs aktív sprint -> a korábban aktív sprint lezártnak jelölve, a dashboard sprint nélkül")
	void noActiveSprintClosesPreviousOne() {
		seedSprint(42, "Regi Sprint", "active", null, null);
		seedLastSyncedAt("2026-10-27T09:30:00Z");

		expectNoActiveSprint();

		syncService.syncNow();

		assertThat(dashboardService.getDashboard().sprint()).isNull();
		String state = jdbc.sql("SELECT state FROM jira_sprint WHERE id = 42").query(String.class).single();
		assertThat(state).isEqualTo("closed");
	}

}
