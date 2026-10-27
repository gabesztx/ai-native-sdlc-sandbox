package com.hugoboard.jira;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JiraSprintPage(List<JiraSprint> values) {
}
