package com.hugoboard.dashboard;

/** Maps Jira's raw {@code statusCategory.key} ("new"/"indeterminate"/"done") to our KPI buckets. */
final class StatusCategoryMapper {

	private StatusCategoryMapper() {
	}

	static String map(String jiraStatusCategoryKey) {
		if (jiraStatusCategoryKey == null) {
			return "TODO";
		}
		return switch (jiraStatusCategoryKey) {
			case "done" -> "DONE";
			case "indeterminate" -> "IN_PROGRESS";
			default -> "TODO";
		};
	}

}
