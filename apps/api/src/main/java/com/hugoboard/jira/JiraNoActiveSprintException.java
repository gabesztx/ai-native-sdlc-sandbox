package com.hugoboard.jira;

/** Distinguishes "Jira has no active sprint right now" (a valid state) from a real fetch failure. */
public class JiraNoActiveSprintException extends JiraImportException {

	public JiraNoActiveSprintException(String message) {
		super(message);
	}

}
