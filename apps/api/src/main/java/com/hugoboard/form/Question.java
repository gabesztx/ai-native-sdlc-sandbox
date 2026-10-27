package com.hugoboard.form;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;

public record Question(
		@Schema(requiredMode = REQUIRED) long id,
		@Schema(requiredMode = REQUIRED) int position,
		@Schema(requiredMode = REQUIRED) String text,
		@Schema(requiredMode = REQUIRED, allowableValues = { "SCALE", "TEXT" }) String type,
		@Schema(requiredMode = REQUIRED) boolean required,
		Integer scaleMin,
		Integer scaleMax) {
}
