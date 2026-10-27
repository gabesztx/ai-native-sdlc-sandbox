package com.hugoboard.auth;

public record CurrentUser(long sessionId, Role role, Long memberId, String name) {

	public static final String REQUEST_ATTRIBUTE = CurrentUser.class.getName();

}
