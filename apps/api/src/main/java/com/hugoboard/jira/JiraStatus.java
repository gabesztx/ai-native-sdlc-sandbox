package com.hugoboard.jira;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JiraStatus(String name, JiraStatusCategory statusCategory) {
}
