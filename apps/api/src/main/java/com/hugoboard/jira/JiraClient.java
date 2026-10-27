package com.hugoboard.jira;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class JiraClient {

	private static final int PAGE_SIZE = 50;

	private final RestClient.Builder restClientBuilder;

	private final JiraProperties properties;

	public JiraClient(RestClient.Builder jiraRestClientBuilder, JiraProperties properties) {
		this.restClientBuilder = jiraRestClientBuilder;
		this.properties = properties;
	}

	public long findActiveSprintId() {
		JiraSprintPage page;
		try {
			page = client().get()
					.uri("/rest/agile/1.0/board/{boardId}/sprint?state=active", properties.boardId())
					.retrieve()
					.body(JiraSprintPage.class);
		} catch (RestClientException e) {
			throw new JiraImportException(
					"Failed to look up the active sprint for board " + properties.boardId(), e);
		}
		if (page == null || page.values() == null || page.values().isEmpty()) {
			throw new JiraImportException("No active sprint found for board " + properties.boardId());
		}
		return page.values().get(0).id();
	}

	public List<JiraUser> fetchAssignees(long sprintId) {
		List<JiraUser> assignees = new ArrayList<>();
		int startAt = 0;
		while (true) {
			JiraIssuePage page = fetchIssuePage(sprintId, startAt);
			if (page == null) {
				break;
			}
			List<JiraIssue> issues = page.issues() == null ? List.of() : page.issues();
			for (JiraIssue issue : issues) {
				JiraUser assignee = issue.assignee();
				if (assignee != null) {
					assignees.add(assignee);
				}
			}
			startAt += issues.size();
			if (issues.isEmpty() || startAt >= page.total()) {
				break;
			}
		}
		return assignees;
	}

	private JiraIssuePage fetchIssuePage(long sprintId, int startAt) {
		try {
			return client().get()
					.uri("/rest/agile/1.0/sprint/{sprintId}/issue?startAt={startAt}&maxResults={maxResults}&fields=assignee",
							sprintId, startAt, PAGE_SIZE)
					.retrieve()
					.body(JiraIssuePage.class);
		} catch (RestClientException e) {
			throw new JiraImportException("Failed to fetch issues for sprint " + sprintId, e);
		}
	}

	/**
	 * Built fresh from the injected (singleton) builder on every call, never cached,
	 * so a test can bind a MockRestServiceServer to that builder before invoking this
	 * client and have it take effect.
	 */
	private RestClient client() {
		return restClientBuilder.clone()
				.baseUrl(properties.baseUrl())
				.defaultHeaders(headers -> headers.setBasicAuth(properties.email(), properties.apiToken()))
				.build();
	}

}
