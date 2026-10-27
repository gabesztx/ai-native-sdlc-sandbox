package com.hugoboard.dashboard;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;

public record Dashboard(
		Sprint sprint,
		@Schema(requiredMode = REQUIRED) Kpis kpis,
		String updatedAt) {

	public record Kpis(
			@Schema(requiredMode = REQUIRED) int total,
			@Schema(requiredMode = REQUIRED) int donePercent,
			@Schema(requiredMode = REQUIRED) int inProgress,
			@Schema(requiredMode = REQUIRED) int blocked,
			@Schema(requiredMode = REQUIRED) int unassigned) {
	}

}
