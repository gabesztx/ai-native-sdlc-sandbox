package com.hugoboard.dashboard;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;

public record Sprint(
		@Schema(requiredMode = REQUIRED) String name,
		String startDate,
		String endDate,
		@Schema(requiredMode = REQUIRED) int elapsedPercent,
		@Schema(requiredMode = REQUIRED) int daysRemaining) {
}
