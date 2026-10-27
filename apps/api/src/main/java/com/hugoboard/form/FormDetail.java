package com.hugoboard.form;

import com.hugoboard.user.TeamMember;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record FormDetail(
		long id,
		String topic,
		TeamMember evaluatedMember,
		@Schema(allowableValues = { "DRAFT", "SENT" }) String status,
		String createdAt,
		String sentAt,
		int recipientCount,
		int submittedCount,
		List<Question> questions,
		List<Recipient> recipients) {

	static FormDetail of(FormSummary summary, List<Question> questions, List<Recipient> recipients) {
		return new FormDetail(summary.id(), summary.topic(), summary.evaluatedMember(), summary.status(),
				summary.createdAt(), summary.sentAt(), summary.recipientCount(), summary.submittedCount(),
				questions, recipients);
	}

}
