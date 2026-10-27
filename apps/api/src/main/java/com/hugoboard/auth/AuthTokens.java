package com.hugoboard.auth;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;

public record AuthTokens(
		@Schema(requiredMode = REQUIRED) String accessToken,
		@Schema(requiredMode = REQUIRED) long accessTokenExpiresIn,
		@Schema(requiredMode = REQUIRED) String refreshToken,
		@Schema(requiredMode = REQUIRED) long refreshTokenExpiresIn,
		@Schema(requiredMode = REQUIRED) Me me) {
}
