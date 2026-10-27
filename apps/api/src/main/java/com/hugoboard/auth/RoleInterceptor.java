package com.hugoboard.auth;

import com.hugoboard.common.ForbiddenException;
import com.hugoboard.common.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
class RoleInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		String path = request.getRequestURI().substring(request.getContextPath().length());
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
		return path.equals("/api/auth/me") || path.equals("/api/auth/logout")
				|| path.startsWith("/api/dashboard");
	}

}
