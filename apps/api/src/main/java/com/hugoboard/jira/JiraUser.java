package com.hugoboard.jira;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JiraUser(
		String accountId,
		String displayName,
		String emailAddress,
		AvatarUrls avatarUrls,
		Boolean active) {

	public boolean isActive() {
		return active == null || active;
	}

	public String avatarUrl() {
		return avatarUrls == null ? null : avatarUrls.size48x48();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record AvatarUrls(@JsonProperty("48x48") String size48x48) {
	}

}
