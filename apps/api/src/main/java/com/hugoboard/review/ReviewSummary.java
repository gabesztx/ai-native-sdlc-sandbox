package com.hugoboard.review;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import com.hugoboard.user.TeamMember;
import io.swagger.v3.oas.annotations.media.Schema;

public record ReviewSummary(
		@Schema(requiredMode = REQUIRED) long id,
		@Schema(requiredMode = REQUIRED) long formId,
		@Schema(requiredMode = REQUIRED) String topic,
		@Schema(requiredMode = REQUIRED) TeamMember evaluatedMember,
		@Schema(requiredMode = REQUIRED, allowableValues = { "PENDING", "SUBMITTED" }) String status,
		String submittedAt) {
}
