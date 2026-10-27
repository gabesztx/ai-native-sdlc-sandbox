package com.hugoboard.form;

import com.hugoboard.user.TeamMember;
import io.swagger.v3.oas.annotations.media.Schema;

public record Recipient(
		TeamMember member,
		@Schema(allowableValues = { "PENDING", "SUBMITTED" }) String status,
		String submittedAt) {
}
