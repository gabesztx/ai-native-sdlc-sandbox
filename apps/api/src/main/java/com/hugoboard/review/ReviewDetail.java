package com.hugoboard.review;

import com.hugoboard.form.Question;
import com.hugoboard.user.TeamMember;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record ReviewDetail(
		long id,
		String topic,
		TeamMember evaluatedMember,
		@Schema(allowableValues = { "PENDING", "SUBMITTED" }) String status,
		String submittedAt,
		List<Question> questions) {
}
