package com.hugoboard.auth;

record AuthSession(
		long id,
		Role role,
		Long memberId,
		String accessExpiresAt,
		String refreshExpiresAt,
		String revokedAt) {
}
