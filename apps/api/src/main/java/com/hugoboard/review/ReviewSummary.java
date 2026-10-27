package com.hugoboard.review;

import com.hugoboard.user.TeamMember;
import io.swagger.v3.oas.annotations.media.Schema;

public record ReviewSummary(
		long id,
		long formId,
		String topic,
		TeamMember evaluatedMember,
		@Schema(allowableValues = { "PENDING", "SUBMITTED" }) String status,
		String submittedAt) {
}
