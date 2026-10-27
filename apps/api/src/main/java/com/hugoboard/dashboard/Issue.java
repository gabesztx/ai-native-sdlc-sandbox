package com.hugoboard.dashboard;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;

public record Issue(
		@Schema(requiredMode = REQUIRED) String key,
		@Schema(requiredMode = REQUIRED) String summary,
		@Schema(requiredMode = REQUIRED) String status,
		@Schema(requiredMode = REQUIRED, allowableValues = { "TODO", "IN_PROGRESS", "DONE" }) String statusCategory,
		Assignee assignee,
		@Schema(requiredMode = REQUIRED) String url) {

	public record Assignee(
			@Schema(requiredMode = REQUIRED) String name,
			String avatarUrl) {
	}

}
