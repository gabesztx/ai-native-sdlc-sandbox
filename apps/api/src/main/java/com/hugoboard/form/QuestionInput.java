package com.hugoboard.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record QuestionInput(
		@NotBlank @Size(max = 500) String text,
		@NotNull @Pattern(regexp = "SCALE|TEXT") @Schema(allowableValues = { "SCALE", "TEXT" }) String type,
		@NotNull Boolean required,
		Integer scaleMin,
		Integer scaleMax) {
}
