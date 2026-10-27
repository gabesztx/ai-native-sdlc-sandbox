package com.hugoboard.jira;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JiraIssue(String id, String key, Fields fields) {

	public JiraUser assignee() {
		return fields == null ? null : fields.assignee();
	}

	public String summary() {
		return fields == null ? null : fields.summary();
	}

	public JiraStatus status() {
		return fields == null ? null : fields.status();
	}

	public String updated() {
		return fields == null ? null : fields.updated();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Fields(String summary, JiraStatus status, JiraUser assignee, String updated) {
	}

}
