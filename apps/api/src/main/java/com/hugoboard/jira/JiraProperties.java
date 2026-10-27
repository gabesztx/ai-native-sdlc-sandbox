package com.hugoboard.jira;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JiraProperties {

	private final String baseUrl;

	private final String email;

	private final String apiToken;

	private final String boardId;

	public JiraProperties(
			@Value("${hugo.jira.base-url:}") String baseUrl,
			@Value("${hugo.jira.email:}") String email,
			@Value("${hugo.jira.api-token:}") String apiToken,
			@Value("${hugo.jira.board-id:}") String boardId) {
		this.baseUrl = baseUrl;
		this.email = email;
		this.apiToken = apiToken;
		this.boardId = boardId;
	}

	public boolean isConfigured() {
		return isSet(baseUrl) && isSet(email) && isSet(apiToken) && isSet(boardId);
	}

	public String baseUrl() {
		return baseUrl;
	}

	public String email() {
		return email;
	}

	public String apiToken() {
		return apiToken;
	}

	public String boardId() {
		return boardId;
	}

	private static boolean isSet(String value) {
		return value != null && !value.isBlank();
	}

}
