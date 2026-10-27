package com.hugoboard.jira;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JiraStatusCategory(String key) {
}
