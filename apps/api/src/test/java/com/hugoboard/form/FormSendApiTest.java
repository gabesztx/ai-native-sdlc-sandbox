package com.hugoboard.form;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hugoboard.support.ApiTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class FormSendApiTest extends ApiTestSupport {

	private String admin;

	private long evaluatedId;

	@BeforeEach
	void setUp() throws Exception {
		admin = adminToken();
		evaluatedId = insertMember("Teszt Elek");
	}

	@Test
	@DisplayName("AC23: kiküldéskor SENT lesz, kitöltődik a sentAt, és N-1 PENDING kitöltés jön létre")
	void sendCreatesAssignmentsForEveryoneButEvaluated() throws Exception {
		insertMember("Minta Anna");
		insertMember("Próba Péter");
		insertMember("Kitalált Kata");
		long formId = createForm(admin, evaluatedId, SCALE_AND_TEXT_QUESTIONS);

		mvc.perform(post("/api/admin/forms/" + formId + "/send").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SENT"))
				.andExpect(jsonPath("$.sentAt", notNullValue()))
				.andExpect(jsonPath("$.recipients", hasSize(3)))
				.andExpect(jsonPath("$.recipients[*].member.name",
						containsInAnyOrder("Minta Anna", "Próba Péter", "Kitalált Kata")))
				.andExpect(jsonPath("$.recipients[*].status", everyItem(is("PENDING"))));
	}

	@Test
	@DisplayName("AC24: SENT űrlap módosítása 409, az űrlap változatlan")
	void cannotUpdateSentForm() throws Exception {
		insertMember("Minta Anna");
		long formId = createForm(admin, evaluatedId, SCALE_AND_TEXT_QUESTIONS);
		sendForm(admin, formId);

		mvc.perform(put("/api/admin/forms/" + formId)
				.header(HttpHeaders.AUTHORIZATION, bearer(admin))
				.contentType(MediaType.APPLICATION_JSON)
				.content(formJson("Új téma", evaluatedId, "[]")))
				.andExpect(status().isConflict());

		mvc.perform(get("/api/admin/forms/" + formId).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andExpect(jsonPath("$.topic").value("Csapatmunka Q4"))
				.andExpect(jsonPath("$.questions", hasSize(2)));
	}

	@Test
	@DisplayName("AC25: SENT űrlap újbóli kiküldése 409, nem jön létre újabb kitöltés")
	void cannotSendTwice() throws Exception {
		insertMember("Minta Anna");
		long formId = createForm(admin, evaluatedId, SCALE_AND_TEXT_QUESTIONS);
		sendForm(admin, formId);

		mvc.perform(post("/api/admin/forms/" + formId + "/send").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andExpect(status().isConflict());

		mvc.perform(get("/api/admin/forms/" + formId).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andExpect(jsonPath("$.recipients", hasSize(1)));
	}

	@Test
	@DisplayName("AC26: kérdés nélküli DRAFT kiküldése 422, DRAFT marad")
	void cannotSendWithoutQuestions() throws Exception {
		insertMember("Minta Anna");
		long formId = createForm(admin, evaluatedId, "[]");

		mvc.perform(post("/api/admin/forms/" + formId + "/send").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andExpect(status().isUnprocessableContent());

		mvc.perform(get("/api/admin/forms/" + formId).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andExpect(jsonPath("$.status").value("DRAFT"));
	}

	@Test
	@DisplayName("AC27: ha az értékelten kívül nincs csapattag, a kiküldés 422, DRAFT marad")
	void cannotSendWithoutRecipients() throws Exception {
		long formId = createForm(admin, evaluatedId, SCALE_AND_TEXT_QUESTIONS);

		mvc.perform(post("/api/admin/forms/" + formId + "/send").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andExpect(status().isUnprocessableContent());

		mvc.perform(get("/api/admin/forms/" + formId).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andExpect(jsonPath("$.status").value("DRAFT"));
	}

	@Test
	@DisplayName("AC16: kiküldéskor az inaktív importált tag nem kap kitöltést, csak az aktívak")
	void sendSkipsInactiveImportedMembers() throws Exception {
		insertMember("Minta Anna", true);
		insertMember("Inaktív Ilona", false);
		long formId = createForm(admin, evaluatedId, SCALE_AND_TEXT_QUESTIONS);

		mvc.perform(post("/api/admin/forms/" + formId + "/send").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.recipients", hasSize(1)))
				.andExpect(jsonPath("$.recipients[0].member.name").value("Minta Anna"));
	}

}
