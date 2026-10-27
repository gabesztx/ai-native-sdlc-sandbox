package com.hugoboard.review;

import com.hugoboard.common.BadRequestException;
import com.hugoboard.common.ConflictException;
import com.hugoboard.common.NotFoundException;
import com.hugoboard.common.Timestamps;
import com.hugoboard.form.FormService;
import com.hugoboard.form.Question;
import java.time.Clock;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

	static final int MAX_TEXT_ANSWER_LENGTH = 2000;

	private final ReviewRepository repository;

	private final FormService formService;

	private final Clock clock;

	public ReviewService(ReviewRepository repository, FormService formService, Clock clock) {
		this.repository = repository;
		this.formService = formService;
		this.clock = clock;
	}

	public List<ReviewSummary> listFor(long memberId) {
		return repository.findForRecipient(memberId);
	}

	public ReviewDetail get(long assignmentId, long memberId) {
		ReviewSummary review = own(assignmentId, memberId);
		return new ReviewDetail(review.id(), review.topic(), review.evaluatedMember(), review.status(),
				review.submittedAt(), formService.questions(review.formId()));
	}

	@Transactional
	public ReviewSummary submit(long assignmentId, long memberId, SubmitInput input) {
		ReviewSummary review = own(assignmentId, memberId);
		if ("SUBMITTED".equals(review.status())) {
			throw new ConflictException("Már beküldted");
		}
		List<Question> questions = formService.questions(review.formId());
		Map<Long, AnswerInput> answers = validate(questions, input.answers());
		for (Question question : questions) {
			AnswerInput answer = answers.get(question.id());
			if (answer != null && hasValue(question, answer)) {
				String text = answer.textValue() == null ? null : answer.textValue().strip();
				repository.insertAnswer(assignmentId, question.id(), answer.scaleValue(), text);
			}
		}
		if (repository.markSubmitted(assignmentId, Timestamps.now(clock)) == 0) {
			throw new ConflictException("Már beküldted");
		}
		return own(assignmentId, memberId);
	}

	public List<FormResponse> responses(long formId) {
		formService.summary(formId);
		return repository.findSubmitted(formId).stream()
				.map(a -> new FormResponse(a.member(), a.submittedAt(), repository.findAnswers(a.id())))
				.toList();
	}

	private ReviewSummary own(long assignmentId, long memberId) {
		return repository.findForRecipient(assignmentId, memberId)
				.orElseThrow(() -> new NotFoundException("Nincs ilyen kitöltés"));
	}

	private Map<Long, AnswerInput> validate(List<Question> questions, List<AnswerInput> input) {
		Map<Long, Question> byId = new HashMap<>();
		questions.forEach(q -> byId.put(q.id(), q));
		Map<String, String> errors = new LinkedHashMap<>();
		Map<Long, AnswerInput> answers = new HashMap<>();
		for (int i = 0; i < input.size(); i++) {
			AnswerInput answer = input.get(i);
			Question question = byId.get(answer.questionId());
			String key = "answers[" + i + "]";
			if (question == null) {
				errors.put(key + ".questionId", "A kérdés nem ehhez az űrlaphoz tartozik");
			}
			else if (answers.putIfAbsent(question.id(), answer) != null) {
				errors.put(key + ".questionId", "Egy kérdésre csak egy válasz adható");
			}
			else {
				String typeError = typeError(question, answer);
				if (typeError != null) {
					errors.put(key, typeError);
				}
			}
		}
		for (Question question : questions) {
			AnswerInput answer = answers.get(question.id());
			if (question.required() && (answer == null || !hasValue(question, answer))) {
				errors.putIfAbsent("question." + question.id(), "Kötelező kérdés");
			}
		}
		if (!errors.isEmpty()) {
			throw new BadRequestException("Invalid request", errors);
		}
		return answers;
	}

	private static String typeError(Question question, AnswerInput answer) {
		if ("SCALE".equals(question.type())) {
			if (answer.textValue() != null) {
				return "Skálás kérdésre számot kell adni";
			}
			Integer value = answer.scaleValue();
			if (value != null && (value < question.scaleMin() || value > question.scaleMax())) {
				return "Az érték " + question.scaleMin() + " és " + question.scaleMax() + " között lehet";
			}
			return null;
		}
		if (answer.scaleValue() != null) {
			return "Szöveges kérdésre szöveget kell adni";
		}
		if (answer.textValue() != null && answer.textValue().length() > MAX_TEXT_ANSWER_LENGTH) {
			return "A válasz legfeljebb " + MAX_TEXT_ANSWER_LENGTH + " karakter lehet";
		}
		return null;
	}

	private static boolean hasValue(Question question, AnswerInput answer) {
		if ("SCALE".equals(question.type())) {
			return answer.scaleValue() != null;
		}
		return answer.textValue() != null && !answer.textValue().isBlank();
	}

}
