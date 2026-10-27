package com.hugoboard.form;

import com.hugoboard.user.TeamMember;
import io.swagger.v3.oas.annotations.media.Schema;

public record FormSummary(
		long id,
		String topic,
		TeamMember evaluatedMember,
		@Schema(allowableValues = { "DRAFT", "SENT" }) String status,
		String createdAt,
		String sentAt,
		int recipientCount,
		int submittedCount) {
}
