package com.hugoboard.review;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class ReviewVisibilityApiTest extends ReviewTestSupport {

	@Test
	@DisplayName("AC39: másik kolléga kitöltésének lekérése és beküldése 404")
	void otherMembersReviewIsNotFound() throws Exception {
		long annasReview = reviewIdOf(memberToken(annaId));
		String peter = memberToken(peterId);

		mvc.perform(get("/api/me/reviews/" + annasReview).header(HttpHeaders.AUTHORIZATION, bearer(peter)))
				.andExpect(status().isNotFound());
		submit(peter, annasReview, validAnswers()).andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("AC40: a saját beküldött kitöltésnél sem a lista, sem a részletek nem tartalmaznak válaszokat")
	void memberNeverReceivesAnswers() throws Exception {
		String anna = memberToken(annaId);
		long reviewId = reviewIdOf(anna);
		submit(anna, reviewId, validAnswers());

		mvc.perform(get("/api/me/reviews").header(HttpHeaders.AUTHORIZATION, bearer(anna)))
				.andExpect(content().string(not(containsString("Több dokumentáció"))))
				.andExpect(jsonPath("$[0].answers").doesNotExist());
		mvc.perform(get("/api/me/reviews/" + reviewId).header(HttpHeaders.AUTHORIZATION, bearer(anna)))
				.andExpect(content().string(not(containsString("Több dokumentáció"))))
				.andExpect(jsonPath("$.answers").doesNotExist());
	}

	@Test
	@DisplayName("AC41: az admin részleteiben a beküldő SUBMITTED, a másik PENDING, submittedCount 1")
	void adminSeesRecipientStatuses() throws Exception {
		String anna = memberToken(annaId);
		submit(anna, reviewIdOf(anna), validAnswers());

		mvc.perform(get("/api/admin/forms/" + formId).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andExpect(jsonPath("$.submittedCount").value(1))
				.andExpect(jsonPath("$.recipients[?(@.member.name == 'Minta Anna')].status").value("SUBMITTED"))
				.andExpect(jsonPath("$.recipients[?(@.member.name == 'Minta Anna')].submittedAt").isNotEmpty())
				.andExpect(jsonPath("$.recipients[?(@.member.name == 'Próba Péter')].status").value("PENDING"));
		mvc.perform(get("/api/admin/forms").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andExpect(jsonPath("$[0].submittedCount").value(1));
	}

	@Test
	@DisplayName("AC42: az admin a beküldött kitöltések nevét, idejét és válaszait látja, a PENDING-et nem")
	void adminSeesSubmittedResponsesOnly() throws Exception {
		String anna = memberToken(annaId);
		submit(anna, reviewIdOf(anna), validAnswers());

		mvc.perform(get("/api/admin/forms/" + formId + "/responses").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].member.name").value("Minta Anna"))
				.andExpect(jsonPath("$[0].submittedAt").isNotEmpty())
				.andExpect(jsonPath("$[0].answers[0].questionId").value(scaleQuestionId))
				.andExpect(jsonPath("$[0].answers[0].scaleValue").value(4))
				.andExpect(jsonPath("$[0].answers[1].textValue").value("Több dokumentáció"));
	}

}
