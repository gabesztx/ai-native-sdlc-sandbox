package com.hugoboard.auth;

import com.hugoboard.common.NotFoundException;
import com.hugoboard.common.UnauthorizedException;
import com.hugoboard.user.TeamMember;
import com.hugoboard.user.TeamMemberService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);

	static final Duration REFRESH_TOKEN_TTL = Duration.ofHours(8);

	private static final String ADMIN_DISPLAY_NAME = "Admin";

	private static final String INVALID_CREDENTIALS = "Hibás e-mail cím vagy jelszó";

	private static final String INVALID_SESSION = "A munkamenet lejárt vagy érvénytelen";

	private final AdminAccountRepository adminAccounts;

	private final AuthSessionRepository sessions;

	private final TeamMemberService teamMembers;

	private final BCryptPasswordEncoder passwordEncoder;

	private final TokenCodec tokens;

	private final Clock clock;

	// Compared against when the e-mail is unknown, so both failure paths cost the same BCrypt work.
	private final String dummyHash;

	public AuthService(AdminAccountRepository adminAccounts, AuthSessionRepository sessions,
			TeamMemberService teamMembers, BCryptPasswordEncoder passwordEncoder, TokenCodec tokens,
			Clock clock) {
		this.adminAccounts = adminAccounts;
		this.sessions = sessions;
		this.teamMembers = teamMembers;
		this.passwordEncoder = passwordEncoder;
		this.tokens = tokens;
		this.clock = clock;
		this.dummyHash = passwordEncoder.encode("unused-dummy-password");
	}

	@Transactional
	public AuthTokens loginAdmin(String email, String password) {
		Optional<AdminAccount> admin = adminAccounts.findByEmail(email);
		String hash = admin.map(AdminAccount::passwordHash).orElse(dummyHash);
		boolean matches = passwordEncoder.matches(password, hash);
		if (admin.isEmpty() || !matches) {
			throw new UnauthorizedException(INVALID_CREDENTIALS);
		}
		return openSession(Role.ADMIN, null, ADMIN_DISPLAY_NAME);
	}

	@Transactional
	public AuthTokens loginMember(long memberId) {
		TeamMember member = teamMembers.findById(memberId)
				.orElseThrow(() -> new NotFoundException("Nincs ilyen csapattag"));
		return openSession(Role.MEMBER, member.id(), member.name());
	}

	@Transactional
	public AuthTokens refresh(String refreshToken) {
		AuthSession session = sessions.findByRefreshTokenHash(tokens.hash(refreshToken))
				.filter(s -> s.revokedAt() == null)
				.filter(s -> now().isBefore(Instant.parse(s.refreshExpiresAt())))
				.orElseThrow(() -> new UnauthorizedException(INVALID_SESSION));
		String accessToken = tokens.newToken();
		String newRefreshToken = tokens.newToken();
		Instant now = now();
		sessions.rotate(session.id(), tokens.hash(accessToken), format(now.plus(ACCESS_TOKEN_TTL)),
				tokens.hash(newRefreshToken), format(now.plus(REFRESH_TOKEN_TTL)));
		return tokensFor(accessToken, newRefreshToken, me(session));
	}

	@Transactional
	public void logout(CurrentUser user) {
		sessions.revoke(user.sessionId(), format(now()));
	}

	public Optional<CurrentUser> authenticate(String accessToken) {
		return sessions.findByAccessTokenHash(tokens.hash(accessToken))
				.filter(s -> s.revokedAt() == null)
				.filter(s -> now().isBefore(Instant.parse(s.accessExpiresAt())))
				.map(s -> {
					Me me = me(s);
					return new CurrentUser(s.id(), me.role(), me.memberId(), me.name());
				});
	}

	private AuthTokens openSession(Role role, Long memberId, String name) {
		String accessToken = tokens.newToken();
		String refreshToken = tokens.newToken();
		Instant now = now();
		sessions.insert(role, memberId, tokens.hash(accessToken), format(now.plus(ACCESS_TOKEN_TTL)),
				tokens.hash(refreshToken), format(now.plus(REFRESH_TOKEN_TTL)), format(now));
		return tokensFor(accessToken, refreshToken, new Me(role, memberId, name));
	}

	private Me me(AuthSession session) {
		if (session.role() == Role.ADMIN) {
			return new Me(Role.ADMIN, null, ADMIN_DISPLAY_NAME);
		}
		String name = teamMembers.findById(session.memberId()).map(TeamMember::name).orElse("");
		return new Me(Role.MEMBER, session.memberId(), name);
	}

	private AuthTokens tokensFor(String accessToken, String refreshToken, Me me) {
		return new AuthTokens(accessToken, ACCESS_TOKEN_TTL.toSeconds(), refreshToken,
				REFRESH_TOKEN_TTL.toSeconds(), me);
	}

	private Instant now() {
		return clock.instant().truncatedTo(ChronoUnit.SECONDS);
	}

	private static String format(Instant instant) {
		return instant.truncatedTo(ChronoUnit.SECONDS).toString();
	}

}
