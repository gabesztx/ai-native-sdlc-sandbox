package com.hugoboard.auth;

import com.hugoboard.common.ForbiddenException;
import com.hugoboard.common.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

@Component
class RoleInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		// Decide on the pattern Spring actually matched, not on the raw URI: an encoded
		// or matrix-parameter path (/api/%61dmin/…, /api/admin;x=1/…) reaches the same
		// handler but would slip past a prefix check on the raw URI.
		Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
		if (!(pattern instanceof String path)) {
			return true;
		}
		Role required = requiredRole(path);
		boolean authenticationRequired = required != null || isAnyUserPath(path);
		if (!authenticationRequired) {
			return true;
		}
		Object attribute = request.getAttribute(CurrentUser.REQUEST_ATTRIBUTE);
		if (!(attribute instanceof CurrentUser user)) {
			throw new UnauthorizedException("Bejelentkezés szükséges");
		}
		if (required != null && user.role() != required) {
			throw new ForbiddenException("Ehhez a művelethez nincs jogosultságod");
		}
		return true;
	}

	private static Role requiredRole(String path) {
		if (path.startsWith("/api/admin/")) {
			return Role.ADMIN;
		}
		if (path.startsWith("/api/me/")) {
			return Role.MEMBER;
		}
		return null;
	}

	private static boolean isAnyUserPath(String path) {
		return path.equals("/api/auth/me") || path.equals("/api/dashboard") || path.startsWith("/api/dashboard/");
	}

}
