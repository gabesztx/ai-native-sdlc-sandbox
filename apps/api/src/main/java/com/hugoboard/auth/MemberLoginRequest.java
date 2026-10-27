package com.hugoboard.auth;

import jakarta.validation.constraints.NotNull;

public record MemberLoginRequest(@NotNull Long memberId) {
}
