package com.hugoboard.review;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;

public record Answer(
		@Schema(requiredMode = REQUIRED) long questionId,
		Integer scaleValue,
		String textValue) {
}
