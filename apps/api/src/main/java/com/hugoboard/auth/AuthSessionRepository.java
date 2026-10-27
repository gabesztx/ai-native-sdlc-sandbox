package com.hugoboard.auth;

import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AuthSessionRepository {

	private static final String SELECT_SESSION = """
			SELECT id, role, member_id, access_expires_at, refresh_expires_at, revoked_at
			FROM auth_session
			""";

	private final JdbcClient jdbc;

	public AuthSessionRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	void insert(Role role, Long memberId, String accessTokenHash, String accessExpiresAt,
			String refreshTokenHash, String refreshExpiresAt, String createdAt) {
		jdbc.sql("""
				INSERT INTO auth_session (role, member_id, access_token_hash, access_expires_at,
				                          refresh_token_hash, refresh_expires_at, created_at)
				VALUES (:role, :memberId, :accessTokenHash, :accessExpiresAt,
				        :refreshTokenHash, :refreshExpiresAt, :createdAt)
				""")
				.param("role", role.name())
				.param("memberId", memberId)
				.param("accessTokenHash", accessTokenHash)
				.param("accessExpiresAt", accessExpiresAt)
				.param("refreshTokenHash", refreshTokenHash)
				.param("refreshExpiresAt", refreshExpiresAt)
				.param("createdAt", createdAt)
				.update();
	}

	Optional<AuthSession> findByAccessTokenHash(String accessTokenHash) {
		return jdbc.sql(SELECT_SESSION + "WHERE access_token_hash = :hash")
				.param("hash", accessTokenHash)
				.query(AuthSession.class)
				.optional();
	}

	Optional<AuthSession> findByRefreshTokenHash(String refreshTokenHash) {
		return jdbc.sql(SELECT_SESSION + "WHERE refresh_token_hash = :hash")
				.param("hash", refreshTokenHash)
				.query(AuthSession.class)
				.optional();
	}

	void rotate(long id, String accessTokenHash, String accessExpiresAt,
			String refreshTokenHash, String refreshExpiresAt) {
		jdbc.sql("""
				UPDATE auth_session
				SET access_token_hash = :accessTokenHash,
				    access_expires_at = :accessExpiresAt,
				    refresh_token_hash = :refreshTokenHash,
				    refresh_expires_at = :refreshExpiresAt
				WHERE id = :id
				""")
				.param("id", id)
				.param("accessTokenHash", accessTokenHash)
				.param("accessExpiresAt", accessExpiresAt)
				.param("refreshTokenHash", refreshTokenHash)
				.param("refreshExpiresAt", refreshExpiresAt)
				.update();
	}

	void revoke(long id, String revokedAt) {
		jdbc.sql("""
				UPDATE auth_session
				SET revoked_at = :revokedAt
				WHERE id = :id AND revoked_at IS NULL
				""")
				.param("id", id)
				.param("revokedAt", revokedAt)
				.update();
	}

}
