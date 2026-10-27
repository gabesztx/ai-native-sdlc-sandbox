package com.hugoboard.form;

import com.hugoboard.common.BadRequestException;
import com.hugoboard.common.ConflictException;
import com.hugoboard.common.NotFoundException;
import com.hugoboard.common.Timestamps;
import com.hugoboard.common.UnprocessableException;
import com.hugoboard.user.TeamMember;
import com.hugoboard.user.TeamMemberService;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FormService {

	private final FormRepository repository;

	private final TeamMemberService teamMembers;

	private final Clock clock;

	public FormService(FormRepository repository, TeamMemberService teamMembers, Clock clock) {
		this.repository = repository;
		this.teamMembers = teamMembers;
		this.clock = clock;
	}

	public List<FormSummary> list() {
		return repository.findAllSummaries();
	}

	public FormDetail get(long formId) {
		FormSummary summary = summary(formId);
		return FormDetail.of(summary, repository.findQuestions(formId), repository.findRecipients(formId));
	}

	public FormSummary summary(long formId) {
		return repository.findSummary(formId)
				.orElseThrow(() -> new NotFoundException("Nincs ilyen űrlap"));
	}

	public List<Question> questions(long formId) {
		return repository.findQuestions(formId);
	}

	@Transactional
	public FormDetail create(FormInput input) {
		validate(input);
		long formId = repository.insertForm(input.topic().strip(), input.evaluatedMemberId(), Timestamps.now(clock));
		repository.insertQuestions(formId, input.questions());
		return get(formId);
	}

	@Transactional
	public FormDetail update(long formId, FormInput input) {
		requireDraft(formId, "A kiküldött űrlap nem módosítható");
		validate(input);
		repository.updateForm(formId, input.topic().strip(), input.evaluatedMemberId());
		repository.deleteQuestions(formId);
		repository.insertQuestions(formId, input.questions());
		return get(formId);
	}

	@Transactional
	public void delete(long formId) {
		requireDraft(formId, "A kiküldött űrlap nem törölhető");
		repository.deleteQuestions(formId);
		repository.deleteForm(formId);
	}

	@Transactional
	public FormDetail send(long formId) {
		FormSummary form = requireDraft(formId, "Az űrlapot már kiküldték");
		if (repository.findQuestions(formId).isEmpty()) {
			throw new UnprocessableException("Legalább egy kérdés szükséges");
		}
		List<TeamMember> recipients = teamMembers.findAll().stream()
				.filter(member -> member.id() != form.evaluatedMember().id())
				.toList();
		if (recipients.isEmpty()) {
			throw new UnprocessableException("Nincs címzett");
		}
		recipients.forEach(member -> repository.insertAssignment(formId, member.id()));
		repository.markSent(formId, Timestamps.now(clock));
		return get(formId);
	}

	private FormSummary requireDraft(long formId, String conflictMessage) {
		FormSummary form = summary(formId);
		if (!"DRAFT".equals(form.status())) {
			throw new ConflictException(conflictMessage);
		}
		return form;
	}

	private void validate(FormInput input) {
		Map<String, String> errors = new LinkedHashMap<>();
		if (teamMembers.findById(input.evaluatedMemberId()).isEmpty()) {
			errors.put("evaluatedMemberId", "Nincs ilyen csapattag");
		}
		for (int i = 0; i < input.questions().size(); i++) {
			QuestionInput q = input.questions().get(i);
			if ("SCALE".equals(q.type())) {
				if (q.scaleMin() == null || q.scaleMax() == null) {
					errors.put("questions[" + i + "].scaleMin", "A skála határai kötelezők");
				}
				else if (q.scaleMin() >= q.scaleMax()) {
					errors.put("questions[" + i + "].scaleMax", "A legnagyobb értéknek nagyobbnak kell lennie");
				}
			}
		}
		if (!errors.isEmpty()) {
			throw new BadRequestException("Invalid request", errors);
		}
	}

}
