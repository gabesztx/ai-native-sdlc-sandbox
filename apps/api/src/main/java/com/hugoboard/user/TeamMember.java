package com.hugoboard.user;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;

public record TeamMember(
		@Schema(requiredMode = REQUIRED) long id,
		@Schema(requiredMode = REQUIRED) String name) {
}
