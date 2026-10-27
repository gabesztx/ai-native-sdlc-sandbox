package com.hugoboard.jira;

/**
 * The message must never include the request's headers or body, since the Jira API
 * token travels in the Authorization header and must not reach the application log.
 */
public class JiraImportException extends RuntimeException {

	public JiraImportException(String message) {
		super(message);
	}

	public JiraImportException(String message, Throwable cause) {
		super(message, cause);
	}

}
