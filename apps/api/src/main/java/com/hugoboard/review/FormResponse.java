package com.hugoboard.review;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import com.hugoboard.user.TeamMember;
import java.util.List;

public record FormResponse(
		@Schema(requiredMode = REQUIRED) TeamMember member,
		String submittedAt,
		@Schema(requiredMode = REQUIRED) List<Answer> answers) {
}
