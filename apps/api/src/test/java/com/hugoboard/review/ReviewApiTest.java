package com.hugoboard.review;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class ReviewApiTest extends ReviewTestSupport {

	@Test
	@DisplayName("AC28: a címzett listájában PENDING állapottal, témával és az értékelt nevével szerepel")
	void recipientSeesPendingReview() throws Exception {
		mvc.perform(get("/api/me/reviews").header(HttpHeaders.AUTHORIZATION, bearer(memberToken(annaId))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].status").value("PENDING"))
				.andExpect(jsonPath("$[0].topic").value("Csapatmunka Q4"))
				.andExpect(jsonPath("$[0].evaluatedMember.name").value("Teszt Elek"));
	}

	@Test
	@DisplayName("AC29: az értékelt személy listájában nem szerepel a róla szóló űrlap")
	void evaluatedDoesNotSeeOwnForm() throws Exception {
		mvc.perform(get("/api/me/reviews").header(HttpHeaders.AUTHORIZATION, bearer(memberToken(evaluatedId))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", empty()));
	}

	@Test
	@DisplayName("AC30: ki nem küldött (DRAFT) űrlap egyik kollégánál sem szerepel")
	void draftIsInvisibleToMembers() throws Exception {
		createForm(admin, annaId, SCALE_AND_TEXT_QUESTIONS);

		mvc.perform(get("/api/me/reviews").header(HttpHeaders.AUTHORIZATION, bearer(memberToken(peterId))))
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].formId").value(formId));
		mvc.perform(get("/api/me/reviews").header(HttpHeaders.AUTHORIZATION, bearer(memberToken(evaluatedId))))
				.andExpect(jsonPath("$", empty()));
	}

	@Test
	@DisplayName("AC31: a saját PENDING kitöltés kérdései sorrendben, típussal, határokkal, kötelezőséggel")
	void pendingReviewShowsQuestions() throws Exception {
		String token = memberToken(annaId);
		long reviewId = reviewIdOf(token);

		mvc.perform(get("/api/me/reviews/" + reviewId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.questions", hasSize(3)))
				.andExpect(jsonPath("$.questions[0].type").value("SCALE"))
				.andExpect(jsonPath("$.questions[0].scaleMin").value(1))
				.andExpect(jsonPath("$.questions[0].scaleMax").value(5))
				.andExpect(jsonPath("$.questions[0].required").value(true))
				.andExpect(jsonPath("$.questions[1].type").value("TEXT"))
				.andExpect(jsonPath("$.questions[2].required").value(false));
	}

}
