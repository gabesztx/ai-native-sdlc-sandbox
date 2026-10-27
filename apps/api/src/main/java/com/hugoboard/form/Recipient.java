package com.hugoboard.form;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import com.hugoboard.user.TeamMember;
import io.swagger.v3.oas.annotations.media.Schema;

public record Recipient(
		@Schema(requiredMode = REQUIRED) TeamMember member,
		@Schema(requiredMode = REQUIRED, allowableValues = { "PENDING", "SUBMITTED" }) String status,
		String submittedAt) {
}
