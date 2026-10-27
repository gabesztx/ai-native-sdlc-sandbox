package com.hugoboard.auth;

public record Me(Role role, Long memberId, String name) {

	static Me of(CurrentUser user) {
		return new Me(user.role(), user.memberId(), user.name());
	}

}
