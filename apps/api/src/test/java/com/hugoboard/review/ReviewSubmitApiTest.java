package com.hugoboard.review;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;

class ReviewSubmitApiTest extends ReviewTestSupport {

	private String anna;

	private long reviewId;

	@BeforeEach
	void setUpReview() throws Exception {
		anna = memberToken(annaId);
		reviewId = reviewIdOf(anna);
	}

	@Test
	@DisplayName("AC32: minden kötelező kérdésre érvényes válasszal a beküldés 200, SUBMITTED, submittedAt")
	void submitsValidAnswers() throws Exception {
		submit(anna, reviewId, validAnswers())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SUBMITTED"))
				.andExpect(jsonPath("$.submittedAt", notNullValue()));
	}

	@Test
	@DisplayName("AC33: kötelező kérdés kihagyásakor 400 a kérdés megjelölésével, PENDING marad")
	void missingRequiredAnswerIsRejected() throws Exception {
		submit(anna, reviewId, "[{\"questionId\":" + scaleQuestionId + ",\"scaleValue\":3}]")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors", hasKey("question." + textQuestionId)));

		assertStillPending();
	}

	@ParameterizedTest
	@ValueSource(strings = { "0", "6", "\"négy\"" })
	@DisplayName("AC34: skálán kívüli érték vagy szöveg 400, PENDING marad")
	void outOfRangeScaleIsRejected(String value) throws Exception {
		submit(anna, reviewId, "[{\"questionId\":" + scaleQuestionId + ",\"scaleValue\":" + value + "},"
				+ "{\"questionId\":" + textQuestionId + ",\"textValue\":\"ok\"}]")
				.andExpect(status().isBadRequest());

		assertStillPending();
	}

	@Test
	@DisplayName("AC34: skálás kérdésre szöveges válasz 400")
	void textOnScaleQuestionIsRejected() throws Exception {
		submit(anna, reviewId, "[{\"questionId\":" + scaleQuestionId + ",\"textValue\":\"négy\"},"
				+ "{\"questionId\":" + textQuestionId + ",\"textValue\":\"ok\"}]")
				.andExpect(status().isBadRequest());

		assertStillPending();
	}

	@Test
	@DisplayName("AC34: szöveges kérdésre szám 400")
	void numberOnTextQuestionIsRejected() throws Exception {
		submit(anna, reviewId, "[{\"questionId\":" + scaleQuestionId + ",\"scaleValue\":3},"
				+ "{\"questionId\":" + textQuestionId + ",\"scaleValue\":3}]")
				.andExpect(status().isBadRequest());

		assertStillPending();
	}

	@Test
	@DisplayName("AC34: nem egész skálaérték (tizedestört) 400, nem kerekedik")
	void nonIntegerScaleValueIsRejected() throws Exception {
		submit(anna, reviewId, "[{\"questionId\":" + scaleQuestionId + ",\"scaleValue\":3.5},"
				+ "{\"questionId\":" + textQuestionId + ",\"textValue\":\"ok\"}]")
				.andExpect(status().isBadRequest());

		assertStillPending();
	}

	@Test
	@DisplayName("Null elem a válaszlistában 400, nem 500")
	void nullAnswerElementIsRejected() throws Exception {
		submit(anna, reviewId, "[null]").andExpect(status().isBadRequest());

		assertStillPending();
	}

	@Test
	@DisplayName("50-nél több válasszal 400, mezőnkénti hibával")
	void rejectsTooManyAnswers() throws Exception {
		StringBuilder answers = new StringBuilder("[");
		for (int i = 0; i < 51; i++) {
			if (i > 0) {
				answers.append(',');
			}
			answers.append("{\"questionId\":").append(scaleQuestionId).append(",\"scaleValue\":1}");
		}
		answers.append(']');

		submit(anna, reviewId, answers.toString())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors", hasKey("answers")));

		assertStillPending();
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "   " })
	@DisplayName("AC35: kötelező szöveges kérdésre üres vagy csak szóköz 400")
	void blankRequiredTextIsRejected(String text) throws Exception {
		submit(anna, reviewId, "[{\"questionId\":" + scaleQuestionId + ",\"scaleValue\":3},"
				+ "{\"questionId\":" + textQuestionId + ",\"textValue\":\"" + text + "\"}]")
				.andExpect(status().isBadRequest());

		assertStillPending();
	}

	@Test
	@DisplayName("AC35: 2000 karakternél hosszabb szöveges válasz 400")
	void tooLongTextIsRejected() throws Exception {
		submit(anna, reviewId, "[{\"questionId\":" + scaleQuestionId + ",\"scaleValue\":3},"
				+ "{\"questionId\":" + textQuestionId + ",\"textValue\":\"" + "x".repeat(2001) + "\"}]")
				.andExpect(status().isBadRequest());

		assertStillPending();
	}

	@Test
	@DisplayName("AC36: másik űrlap kérdésére adott válasz 400")
	void answerToForeignQuestionIsRejected() throws Exception {
		long otherForm = createForm(admin, annaId, SCALE_AND_TEXT_QUESTIONS);
		String detail = mvc.perform(get("/api/admin/forms/" + otherForm).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andReturn().getResponse().getContentAsString();
		long foreignQuestion = ((Number) com.jayway.jsonpath.JsonPath.read(detail, "$.questions[1].id")).longValue();

		submit(anna, reviewId, validAnswers().replace("]", ",{\"questionId\":" + foreignQuestion
				+ ",\"textValue\":\"idegen\"}]"))
				.andExpect(status().isBadRequest());

		assertStillPending();
	}

	@Test
	@DisplayName("AC37: opcionális kérdés válasz nélkül is beküldhető")
	void optionalQuestionCanBeSkipped() throws Exception {
		submit(anna, reviewId, validAnswers()).andExpect(status().isOk());
	}

	@Test
	@DisplayName("AC38: beküldött kitöltés újbóli beküldése 409, a korábbi válaszok változatlanok")
	void cannotSubmitTwice() throws Exception {
		submit(anna, reviewId, validAnswers()).andExpect(status().isOk());

		submit(anna, reviewId, "[{\"questionId\":" + scaleQuestionId + ",\"scaleValue\":1},"
				+ "{\"questionId\":" + textQuestionId + ",\"textValue\":\"más\"}]")
				.andExpect(status().isConflict());

		mvc.perform(get("/api/admin/forms/" + formId + "/responses").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andExpect(jsonPath("$[0].answers[0].scaleValue").value(4))
				.andExpect(jsonPath("$[0].answers[1].textValue").value("Több dokumentáció"));
	}

	private void assertStillPending() throws Exception {
		mvc.perform(get("/api/me/reviews/" + reviewId).header(HttpHeaders.AUTHORIZATION, bearer(anna)))
				.andExpect(jsonPath("$.status").value("PENDING"));
	}

}
