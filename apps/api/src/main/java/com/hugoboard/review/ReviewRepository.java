package com.hugoboard.review;

import com.hugoboard.user.TeamMember;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ReviewRepository {

	private static final String SELECT_SUMMARY = """
			SELECT a.id, a.form_id, a.status, a.submitted_at, f.topic,
			       m.id AS evaluated_id, m.name AS evaluated_name
			FROM review_assignment a
			JOIN review_form f ON f.id = a.form_id
			JOIN team_member m ON m.id = f.evaluated_member_id
			""";

	private final JdbcClient jdbc;

	public ReviewRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	List<ReviewSummary> findForRecipient(long memberId) {
		return jdbc.sql(SELECT_SUMMARY + """
				WHERE a.recipient_member_id = :memberId
				ORDER BY f.sent_at DESC, a.id DESC
				""")
				.param("memberId", memberId)
				.query(ReviewRepository::mapSummary)
				.list();
	}

	Optional<ReviewSummary> findForRecipient(long assignmentId, long memberId) {
		return jdbc.sql(SELECT_SUMMARY + "WHERE a.id = :id AND a.recipient_member_id = :memberId")
				.param("id", assignmentId)
				.param("memberId", memberId)
				.query(ReviewRepository::mapSummary)
				.optional();
	}

	void insertAnswer(long assignmentId, long questionId, Integer scaleValue, String textValue) {
		jdbc.sql("""
				INSERT INTO answer (assignment_id, question_id, scale_value, text_value)
				VALUES (:assignmentId, :questionId, :scaleValue, :textValue)
				""")
				.param("assignmentId", assignmentId)
				.param("questionId", questionId)
				.param("scaleValue", scaleValue)
				.param("textValue", textValue)
				.update();
	}

	int markSubmitted(long assignmentId, String submittedAt) {
		return jdbc.sql("""
				UPDATE review_assignment
				SET status = 'SUBMITTED', submitted_at = :submittedAt
				WHERE id = :id AND status = 'PENDING'
				""")
				.param("id", assignmentId)
				.param("submittedAt", submittedAt)
				.update();
	}

	List<SubmittedAssignment> findSubmitted(long formId) {
		return jdbc.sql("""
				SELECT a.id, a.submitted_at, m.id AS member_id, m.name AS member_name
				FROM review_assignment a
				JOIN team_member m ON m.id = a.recipient_member_id
				WHERE a.form_id = :formId AND a.status = 'SUBMITTED'
				ORDER BY a.submitted_at, a.id
				""")
				.param("formId", formId)
				.query((rs, n) -> new SubmittedAssignment(rs.getLong("id"),
						new TeamMember(rs.getLong("member_id"), rs.getString("member_name")),
						rs.getString("submitted_at")))
				.list();
	}

	List<Answer> findAnswers(long assignmentId) {
		return jdbc.sql("""
				SELECT an.question_id, an.scale_value, an.text_value
				FROM answer an
				JOIN form_question q ON q.id = an.question_id
				WHERE an.assignment_id = :assignmentId
				ORDER BY q.position
				""")
				.param("assignmentId", assignmentId)
				.query((rs, n) -> new Answer(rs.getLong("question_id"), nullableInt(rs, "scale_value"),
						rs.getString("text_value")))
				.list();
	}

	record SubmittedAssignment(long id, TeamMember member, String submittedAt) {
	}

	private static ReviewSummary mapSummary(ResultSet rs, int rowNum) throws SQLException {
		return new ReviewSummary(rs.getLong("id"), rs.getLong("form_id"), rs.getString("topic"),
				new TeamMember(rs.getLong("evaluated_id"), rs.getString("evaluated_name")),
				rs.getString("status"), rs.getString("submitted_at"));
	}

	private static Integer nullableInt(ResultSet rs, String column) throws SQLException {
		int value = rs.getInt(column);
		return rs.wasNull() ? null : value;
	}

}
