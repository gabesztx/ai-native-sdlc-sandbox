package com.hugoboard.jira;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JiraIssuePage(int startAt, int maxResults, int total, List<JiraIssue> issues) {
}
