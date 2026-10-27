package com.hugoboard.form;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import com.hugoboard.user.TeamMember;
import io.swagger.v3.oas.annotations.media.Schema;

public record FormSummary(
		@Schema(requiredMode = REQUIRED) long id,
		@Schema(requiredMode = REQUIRED) String topic,
		@Schema(requiredMode = REQUIRED) TeamMember evaluatedMember,
		@Schema(requiredMode = REQUIRED, allowableValues = { "DRAFT", "SENT" }) String status,
		@Schema(requiredMode = REQUIRED) String createdAt,
		String sentAt,
		@Schema(requiredMode = REQUIRED) int recipientCount,
		@Schema(requiredMode = REQUIRED) int submittedCount) {
}
