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
				ORDER BY name, id
				""")
				.query(TeamMember.class)
				.list();
	}

	public Optional<TeamMember> findById(long id) {
		return jdbc.sql("""
				SELECT id, name
				FROM team_member
				WHERE id = :id
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

}
