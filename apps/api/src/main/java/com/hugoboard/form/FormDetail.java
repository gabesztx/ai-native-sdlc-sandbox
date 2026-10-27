package com.hugoboard.form;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import com.hugoboard.user.TeamMember;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record FormDetail(
		@Schema(requiredMode = REQUIRED) long id,
		@Schema(requiredMode = REQUIRED) String topic,
		@Schema(requiredMode = REQUIRED) TeamMember evaluatedMember,
		@Schema(requiredMode = REQUIRED, allowableValues = { "DRAFT", "SENT" }) String status,
		@Schema(requiredMode = REQUIRED) String createdAt,
		String sentAt,
		@Schema(requiredMode = REQUIRED) int recipientCount,
		@Schema(requiredMode = REQUIRED) int submittedCount,
		@Schema(requiredMode = REQUIRED) List<Question> questions,
		@Schema(requiredMode = REQUIRED) List<Recipient> recipients) {

	static FormDetail of(FormSummary summary, List<Question> questions, List<Recipient> recipients) {
		return new FormDetail(summary.id(), summary.topic(), summary.evaluatedMember(), summary.status(),
				summary.createdAt(), summary.sentAt(), summary.recipientCount(), summary.submittedCount(),
				questions, recipients);
	}

}
