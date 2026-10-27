package com.hugoboard.jira;

import java.time.Duration;

public class JiraRateLimitException extends JiraImportException {

	private final Duration retryAfter;

	public JiraRateLimitException(String message, Duration retryAfter, Throwable cause) {
		super(message, cause);
		this.retryAfter = retryAfter;
	}

	public Duration retryAfter() {
		return retryAfter;
	}

}
