package com.hugoboard.form;

import com.hugoboard.user.TeamMember;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class FormRepository {

	private static final String SELECT_SUMMARY = """
			SELECT f.id, f.topic, f.status, f.created_at, f.sent_at,
			       m.id AS evaluated_id, m.name AS evaluated_name,
			       (SELECT COUNT(*) FROM review_assignment a WHERE a.form_id = f.id) AS recipient_count,
			       (SELECT COUNT(*) FROM review_assignment a
			         WHERE a.form_id = f.id AND a.status = 'SUBMITTED') AS submitted_count
			FROM review_form f
			JOIN team_member m ON m.id = f.evaluated_member_id
			""";

	private final JdbcClient jdbc;

	public FormRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	List<FormSummary> findAllSummaries() {
		return jdbc.sql(SELECT_SUMMARY + "ORDER BY f.created_at DESC, f.id DESC")
				.query(FormRepository::mapSummary)
				.list();
	}

	Optional<FormSummary> findSummary(long formId) {
		return jdbc.sql(SELECT_SUMMARY + "WHERE f.id = :id")
				.param("id", formId)
				.query(FormRepository::mapSummary)
				.optional();
	}

	List<Question> findQuestions(long formId) {
		return jdbc.sql("""
				SELECT id, position, text, type, required, scale_min, scale_max
				FROM form_question
				WHERE form_id = :formId
				ORDER BY position
				""")
				.param("formId", formId)
				.query((rs, n) -> new Question(rs.getLong("id"), rs.getInt("position"), rs.getString("text"),
						rs.getString("type"), rs.getInt("required") == 1, nullableInt(rs, "scale_min"),
						nullableInt(rs, "scale_max")))
				.list();
	}

	List<Recipient> findRecipients(long formId) {
		return jdbc.sql("""
				SELECT m.id, m.name, a.status, a.submitted_at
				FROM review_assignment a
				JOIN team_member m ON m.id = a.recipient_member_id
				WHERE a.form_id = :formId
				ORDER BY m.name, m.id
				""")
				.param("formId", formId)
				.query((rs, n) -> new Recipient(new TeamMember(rs.getLong("id"), rs.getString("name")),
						rs.getString("status"), rs.getString("submitted_at")))
				.list();
	}

	long insertForm(String topic, long evaluatedMemberId, String createdAt) {
		jdbc.sql("""
				INSERT INTO review_form (topic, evaluated_member_id, status, created_at)
				VALUES (:topic, :evaluatedMemberId, 'DRAFT', :createdAt)
				""")
				.param("topic", topic)
				.param("evaluatedMemberId", evaluatedMemberId)
				.param("createdAt", createdAt)
				.update();
		return jdbc.sql("SELECT last_insert_rowid()").query(Long.class).single();
	}

	void updateForm(long formId, String topic, long evaluatedMemberId) {
		jdbc.sql("""
				UPDATE review_form
				SET topic = :topic, evaluated_member_id = :evaluatedMemberId
				WHERE id = :id
				""")
				.param("id", formId)
				.param("topic", topic)
				.param("evaluatedMemberId", evaluatedMemberId)
				.update();
	}

	void insertQuestions(long formId, List<QuestionInput> questions) {
		for (int i = 0; i < questions.size(); i++) {
			QuestionInput q = questions.get(i);
			boolean scale = "SCALE".equals(q.type());
			jdbc.sql("""
					INSERT INTO form_question (form_id, position, text, type, required, scale_min, scale_max)
					VALUES (:formId, :position, :text, :type, :required, :scaleMin, :scaleMax)
					""")
					.param("formId", formId)
					.param("position", i + 1)
					.param("text", q.text())
					.param("type", q.type())
					.param("required", Boolean.TRUE.equals(q.required()) ? 1 : 0)
					.param("scaleMin", scale ? q.scaleMin() : null)
					.param("scaleMax", scale ? q.scaleMax() : null)
					.update();
		}
	}

	void deleteQuestions(long formId) {
		jdbc.sql("DELETE FROM form_question WHERE form_id = :formId")
				.param("formId", formId)
				.update();
	}

	void deleteForm(long formId) {
		jdbc.sql("DELETE FROM review_form WHERE id = :id")
				.param("id", formId)
				.update();
	}

	void insertAssignment(long formId, long recipientMemberId) {
		jdbc.sql("""
				INSERT INTO review_assignment (form_id, recipient_member_id, status)
				VALUES (:formId, :recipientMemberId, 'PENDING')
				""")
				.param("formId", formId)
				.param("recipientMemberId", recipientMemberId)
				.update();
	}

	void markSent(long formId, String sentAt) {
		jdbc.sql("""
				UPDATE review_form
				SET status = 'SENT', sent_at = :sentAt
				WHERE id = :id
				""")
				.param("id", formId)
				.param("sentAt", sentAt)
				.update();
	}

	private static FormSummary mapSummary(ResultSet rs, int rowNum) throws SQLException {
		return new FormSummary(rs.getLong("id"), rs.getString("topic"),
				new TeamMember(rs.getLong("evaluated_id"), rs.getString("evaluated_name")),
				rs.getString("status"), rs.getString("created_at"), rs.getString("sent_at"),
				rs.getInt("recipient_count"), rs.getInt("submitted_count"));
	}

	private static Integer nullableInt(ResultSet rs, String column) throws SQLException {
		int value = rs.getInt(column);
		return rs.wasNull() ? null : value;
	}

}
