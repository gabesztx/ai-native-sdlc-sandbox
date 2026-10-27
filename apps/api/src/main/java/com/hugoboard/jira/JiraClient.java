package com.hugoboard.jira;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class JiraClient {

	private static final int PAGE_SIZE = 50;

	private static final long DEFAULT_RETRY_AFTER_SECONDS = 60;

	private final RestClient.Builder restClientBuilder;

	private final JiraProperties properties;

	public JiraClient(RestClient.Builder jiraRestClientBuilder, JiraProperties properties) {
		this.restClientBuilder = jiraRestClientBuilder;
		this.properties = properties;
	}

	public long findActiveSprintId() {
		return findActiveSprint().id();
	}

	public JiraSprint findActiveSprint() {
		JiraSprintPage page;
		try {
			page = client().get()
					.uri("/rest/agile/1.0/board/{boardId}/sprint?state=active", properties.boardId())
					.retrieve()
					.body(JiraSprintPage.class);
		} catch (RestClientException e) {
			throw mapError(e, "Failed to look up the active sprint for board " + properties.boardId());
		}
		if (page == null || page.values() == null || page.values().isEmpty()) {
			throw new JiraNoActiveSprintException("No active sprint found for board " + properties.boardId());
		}
		return page.values().get(0);
	}

	public List<JiraUser> fetchAssignees(long sprintId) {
		List<JiraUser> assignees = new ArrayList<>();
		for (JiraIssue issue : fetchIssues(sprintId, "assignee")) {
			JiraUser assignee = issue.assignee();
			if (assignee != null) {
				assignees.add(assignee);
			}
		}
		return assignees;
	}

	/** Full-field fetch of every issue currently in the sprint — used for the initial load and sprint transitions. */
	public List<JiraIssue> fetchActiveSprintIssues(long sprintId) {
		return fetchIssues(sprintId, "summary,status,assignee,updated");
	}

	/** Cheap, key-only fetch of every issue currently in the sprint — used to detect removed/deleted issues. */
	public List<String> fetchActiveSprintIssueKeys(long sprintId) {
		List<String> keys = new ArrayList<>();
		for (JiraIssue issue : fetchIssues(sprintId, "key")) {
			keys.add(issue.key());
		}
		return keys;
	}

	/** Only the issues in the sprint changed since {@code updatedAtIso} (JQL delta fetch). */
	public List<JiraIssue> fetchIssuesUpdatedSince(long sprintId, String updatedAtIso) {
		List<JiraIssue> all = new ArrayList<>();
		int startAt = 0;
		String jql = "updated >= \"" + updatedAtIso + "\"";
		while (true) {
			JiraIssuePage page = fetchIssuePage(
					"/rest/agile/1.0/sprint/{sprintId}/issue?startAt={startAt}&maxResults={maxResults}"
							+ "&fields=summary,status,assignee,updated&jql={jql}",
					sprintId, startAt, PAGE_SIZE, jql);
			if (page == null) {
				break;
			}
			List<JiraIssue> issues = page.issues() == null ? List.of() : page.issues();
			all.addAll(issues);
			startAt += issues.size();
			if (issues.isEmpty() || startAt >= page.total()) {
				break;
			}
		}
		return all;
	}

	/**
	 * Whether an issue still exists in Jira at all, regardless of which sprint (if any) it is
	 * in now — used to tell "left the active sprint" (AC13, still exists) apart from
	 * "deleted in Jira" (AC14, gone) for a key that disappeared from the active sprint's
	 * live key list.
	 */
	public boolean issueExists(String issueKey) {
		try {
			client().get()
					.uri("/rest/api/3/issue/{issueKey}?fields=key", issueKey)
					.retrieve()
					.toBodilessEntity();
			return true;
		} catch (HttpClientErrorException.NotFound e) {
			return false;
		} catch (RestClientException e) {
			throw mapError(e, "Failed to check whether issue " + issueKey + " still exists");
		}
	}

	private List<JiraIssue> fetchIssues(long sprintId, String fields) {
		List<JiraIssue> all = new ArrayList<>();
		int startAt = 0;
		while (true) {
			JiraIssuePage page = fetchIssuePage(
					"/rest/agile/1.0/sprint/{sprintId}/issue?startAt={startAt}&maxResults={maxResults}&fields={fields}",
					sprintId, startAt, PAGE_SIZE, fields);
			if (page == null) {
				break;
			}
			List<JiraIssue> issues = page.issues() == null ? List.of() : page.issues();
			all.addAll(issues);
			startAt += issues.size();
			if (issues.isEmpty() || startAt >= page.total()) {
				break;
			}
		}
		return all;
	}

	private JiraIssuePage fetchIssuePage(String uriTemplate, Object... uriVariables) {
		try {
			return client().get()
					.uri(uriTemplate, uriVariables)
					.retrieve()
					.body(JiraIssuePage.class);
		} catch (RestClientException e) {
			throw mapError(e, "Failed to fetch issues for sprint " + uriVariables[0]);
		}
	}

	private JiraImportException mapError(RestClientException e, String message) {
		if (e instanceof RestClientResponseException responseException
				&& responseException.getStatusCode().value() == 429) {
			return new JiraRateLimitException(message, retryAfter(responseException), e);
		}
		return new JiraImportException(message, e);
	}

	private static Duration retryAfter(RestClientResponseException e) {
		String header = e.getResponseHeaders() == null ? null
				: e.getResponseHeaders().getFirst(HttpHeaders.RETRY_AFTER);
		if (header != null) {
			try {
				return Duration.ofSeconds(Long.parseLong(header.trim()));
			} catch (NumberFormatException ignored) {
				// fall through to the default below
			}
		}
		return Duration.ofSeconds(DEFAULT_RETRY_AFTER_SECONDS);
	}

	/**
	 * Built fresh from the injected (singleton) builder on every call, never cached, so a
	 * test can bind a MockRestServiceServer to that builder before invoking this client and
	 * have it take effect.
	 */
	private RestClient client() {
		return restClientBuilder.clone()
				.baseUrl(properties.baseUrl())
				.defaultHeaders(headers -> headers.setBasicAuth(properties.email(), properties.apiToken()))
				.build();
	}

}
