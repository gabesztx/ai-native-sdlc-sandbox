package com.hugoboard.form;

import io.swagger.v3.oas.annotations.media.Schema;

public record Question(
		long id,
		int position,
		String text,
		@Schema(allowableValues = { "SCALE", "TEXT" }) String type,
		boolean required,
		Integer scaleMin,
		Integer scaleMax) {
}
