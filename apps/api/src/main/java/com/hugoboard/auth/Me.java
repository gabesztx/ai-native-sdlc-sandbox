package com.hugoboard.auth;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;

public record Me(
		@Schema(requiredMode = REQUIRED) Role role,
		Long memberId,
		@Schema(requiredMode = REQUIRED) String name) {

	static Me of(CurrentUser user) {
		return new Me(user.role(), user.memberId(), user.name());
	}

}
