package com.hugoboard.jira;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JiraIssue(Fields fields) {

	public JiraUser assignee() {
		return fields == null ? null : fields.assignee();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Fields(JiraUser assignee) {
	}

}
