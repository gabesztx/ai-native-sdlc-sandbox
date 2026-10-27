package com.hugoboard.user;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class TeamMemberRepository {

	private final JdbcClient jdbc;

	public TeamMemberRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public List<TeamMember> findAll() {
		return jdbc.sql("""
				SELECT id, name
				FROM team_member
				WHERE active = 1
				ORDER BY name, id
				""")
				.query(TeamMember.class)
				.list();
	}

	public Optional<TeamMember> findById(long id) {
		return jdbc.sql("""
				SELECT id, name
				FROM team_member
				WHERE id = :id AND active = 1
				""")
				.param("id", id)
				.query(TeamMember.class)
				.optional();
	}

	public long count() {
		return jdbc.sql("SELECT COUNT(*) FROM team_member")
				.query(Long.class)
				.single();
	}

	public void insert(String name) {
		jdbc.sql("INSERT INTO team_member (name) VALUES (:name)")
				.param("name", name)
				.update();
	}

	public void insertImported(ImportedTeamMember member) {
		jdbc.sql("""
				INSERT INTO team_member (name, jira_account_id, email, avatar_url, active, imported_at)
				VALUES (:name, :jiraAccountId, :email, :avatarUrl, :active, :importedAt)
				""")
				.param("name", member.name())
				.param("jiraAccountId", member.jiraAccountId())
				.param("email", member.email())
				.param("avatarUrl", member.avatarUrl())
				.param("active", member.active() ? 1 : 0)
				.param("importedAt", member.importedAt())
				.update();
	}

}
