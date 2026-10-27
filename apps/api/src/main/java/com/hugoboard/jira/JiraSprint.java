package com.hugoboard.jira;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JiraSprint(long id, String name, String state, String startDate, String endDate) {
}
