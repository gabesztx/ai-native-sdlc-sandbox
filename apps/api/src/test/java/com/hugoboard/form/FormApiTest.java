package com.hugoboard.form;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hugoboard.support.ApiTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class FormApiTest extends ApiTestSupport {

	private String admin;

	private long evaluatedId;

	@BeforeEach
	void setUp() throws Exception {
		admin = adminToken();
		evaluatedId = insertMember("Teszt Elek");
	}

	@Test
	@DisplayName("AC17: érvényes űrlap létrehozása 201, DRAFT, a kérdések sorrendben és beállításokkal")
	void createsDraftForm() throws Exception {
		postForm(formJson("Csapatmunka Q4", evaluatedId, SCALE_AND_TEXT_QUESTIONS))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("DRAFT"))
				.andExpect(jsonPath("$.topic").value("Csapatmunka Q4"))
				.andExpect(jsonPath("$.evaluatedMember.id").value(evaluatedId))
				.andExpect(jsonPath("$.questions", hasSize(2)))
				.andExpect(jsonPath("$.questions[0].position").value(1))
				.andExpect(jsonPath("$.questions[0].type").value("SCALE"))
				.andExpect(jsonPath("$.questions[0].required").value(true))
				.andExpect(jsonPath("$.questions[0].scaleMin").value(1))
				.andExpect(jsonPath("$.questions[0].scaleMax").value(5))
				.andExpect(jsonPath("$.questions[1].position").value(2))
				.andExpect(jsonPath("$.questions[1].type").value("TEXT"))
				.andExpect(jsonPath("$.questions[1].required").value(false));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"{\"topic\":\" \",\"evaluatedMemberId\":%d,\"questions\":[]}",
			"{\"topic\":\"T\",\"evaluatedMemberId\":999999,\"questions\":[]}",
			"{\"topic\":\"T\",\"evaluatedMemberId\":%d,\"questions\":[{\"text\":\"\",\"type\":\"TEXT\",\"required\":true}]}",
			"{\"topic\":\"T\",\"evaluatedMemberId\":%d,\"questions\":[{\"text\":\"Q\",\"type\":\"STAR\",\"required\":true}]}",
			"{\"topic\":\"T\",\"evaluatedMemberId\":%d,\"questions\":[{\"text\":\"Q\",\"type\":\"SCALE\",\"required\":true,\"scaleMin\":1}]}",
			"{\"topic\":\"T\",\"evaluatedMemberId\":%d,\"questions\":[{\"text\":\"Q\",\"type\":\"SCALE\",\"required\":true,\"scaleMin\":5,\"scaleMax\":5}]}" })
	@DisplayName("AC18: hiányos adatokkal 400 mezőnkénti hibával, és nem jön létre űrlap")
	void rejectsInvalidForm(String template) throws Exception {
		postForm(template.formatted(evaluatedId))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors").isMap());

		mvc.perform(get("/api/admin/forms").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	@DisplayName("AC18: túl hosszú téma (200 felett) és kérdés (500 felett) 400")
	void rejectsTooLongTexts() throws Exception {
		postForm(formJson("x".repeat(201), evaluatedId, "[]"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors", hasKey("topic")));
		postForm(formJson("T", evaluatedId,
				"[{\"text\":\"" + "x".repeat(501) + "\",\"type\":\"TEXT\",\"required\":true}]"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors", hasKey("questions[0].text")));
	}

	@Test
	@DisplayName("AC19: DRAFT űrlap témája és kérdései módosíthatók")
	void updatesDraft() throws Exception {
		long formId = createForm(admin, evaluatedId, SCALE_AND_TEXT_QUESTIONS);

		mvc.perform(put("/api/admin/forms/" + formId)
				.header(HttpHeaders.AUTHORIZATION, bearer(admin))
				.contentType(MediaType.APPLICATION_JSON)
				.content(formJson("Új téma", evaluatedId,
						"[{\"text\":\"Egyetlen kérdés\",\"type\":\"TEXT\",\"required\":true}]")))
				.andExpect(status().isOk());

		mvc.perform(get("/api/admin/forms/" + formId).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andExpect(jsonPath("$.topic").value("Új téma"))
				.andExpect(jsonPath("$.questions", hasSize(1)))
				.andExpect(jsonPath("$.questions[0].text").value("Egyetlen kérdés"));
	}

	@Test
	@DisplayName("AC20: a listában látszik a téma, az értékelt, az állapot és a számlálók")
	void listsFormsWithCounts() throws Exception {
		insertMember("Minta Anna");
		insertMember("Próba Péter");
		long sent = createForm(admin, evaluatedId, SCALE_AND_TEXT_QUESTIONS);
		sendForm(admin, sent);
		createForm(admin, evaluatedId, SCALE_AND_TEXT_QUESTIONS);

		mvc.perform(get("/api/admin/forms").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[?(@.id == " + sent + ")].status").value("SENT"))
				.andExpect(jsonPath("$[?(@.id == " + sent + ")].recipientCount").value(2))
				.andExpect(jsonPath("$[?(@.id == " + sent + ")].submittedCount").value(0))
				.andExpect(jsonPath("$[?(@.id == " + sent + ")].topic").value("Csapatmunka Q4"))
				.andExpect(jsonPath("$[?(@.id == " + sent + ")].evaluatedMember.name").value("Teszt Elek"))
				.andExpect(jsonPath("$[?(@.id != " + sent + ")].status").value("DRAFT"));
	}

	@Test
	@DisplayName("AC21: DRAFT űrlap törlése 204, utána nincs a listában és a lekérése 404")
	void deletesDraft() throws Exception {
		long formId = createForm(admin, evaluatedId, SCALE_AND_TEXT_QUESTIONS);

		mvc.perform(delete("/api/admin/forms/" + formId).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andExpect(status().isNoContent());

		mvc.perform(get("/api/admin/forms").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andExpect(jsonPath("$", hasSize(0)));
		mvc.perform(get("/api/admin/forms/" + formId).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("AC22: SENT űrlap törlése 409, az űrlap és a kitöltései változatlanok")
	void cannotDeleteSentForm() throws Exception {
		insertMember("Minta Anna");
		long formId = createForm(admin, evaluatedId, SCALE_AND_TEXT_QUESTIONS);
		sendForm(admin, formId);

		mvc.perform(delete("/api/admin/forms/" + formId).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andExpect(status().isConflict());

		mvc.perform(get("/api/admin/forms/" + formId).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SENT"))
				.andExpect(jsonPath("$.questions", hasSize(2)))
				.andExpect(jsonPath("$.recipients", hasSize(1)));
	}

	private ResultActions postForm(String body) throws Exception {
		return mvc.perform(post("/api/admin/forms")
				.header(HttpHeaders.AUTHORIZATION, bearer(admin))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

}
