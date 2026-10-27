package com.hugoboard.auth;

public record AuthTokens(
		String accessToken,
		long accessTokenExpiresIn,
		String refreshToken,
		long refreshTokenExpiresIn,
		Me me) {
}
