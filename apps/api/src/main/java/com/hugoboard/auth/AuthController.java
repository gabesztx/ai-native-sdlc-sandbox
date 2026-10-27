package com.hugoboard.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/admin/login")
	@Operation(operationId = "loginAdmin")
	public AuthTokens loginAdmin(@Valid @RequestBody AdminLoginRequest request) {
		return authService.loginAdmin(request.email(), request.password());
	}

	@PostMapping("/member/login")
	@Operation(operationId = "loginMember")
	public AuthTokens loginMember(@Valid @RequestBody MemberLoginRequest request) {
		return authService.loginMember(request.memberId());
	}

	@PostMapping("/refresh")
	@Operation(operationId = "refreshTokens")
	public AuthTokens refresh(@Valid @RequestBody RefreshRequest request) {
		return authService.refresh(request.refreshToken());
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(operationId = "logout")
	public void logout(CurrentUser user, @RequestBody(required = false) LogoutRequest request) {
		authService.logout(user);
	}

	@GetMapping("/me")
	@Operation(operationId = "getMe")
	public Me me(CurrentUser user) {
		return Me.of(user);
	}

}
