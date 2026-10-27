package com.hugoboard.review;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.hugoboard.support.ApiTestSupport;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

abstract class ReviewTestSupport extends ApiTestSupport {

	protected String admin;

	protected long evaluatedId;

	protected long annaId;

	protected long peterId;

	protected long formId;

	protected long scaleQuestionId;

	protected long textQuestionId;

	@BeforeEach
	void setUpSentForm() throws Exception {
		admin = adminToken();
		evaluatedId = insertMember("Teszt Elek");
		annaId = insertMember("Minta Anna");
		peterId = insertMember("Próba Péter");
		formId = createForm(admin, evaluatedId, """
				[{"text":"Mennyire segítőkész?","type":"SCALE","required":true,"scaleMin":1,"scaleMax":5},
				 {"text":"Miben fejlődhetne?","type":"TEXT","required":true},
				 {"text":"Egyéb megjegyzés","type":"TEXT","required":false}]""");
		sendForm(admin, formId);
		String detail = mvc.perform(get("/api/admin/forms/" + formId).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andReturn().getResponse().getContentAsString();
		scaleQuestionId = ((Number) JsonPath.read(detail, "$.questions[0].id")).longValue();
		textQuestionId = ((Number) JsonPath.read(detail, "$.questions[1].id")).longValue();
	}

	protected long reviewIdOf(String memberToken) throws Exception {
		String list = mvc.perform(get("/api/me/reviews").header(HttpHeaders.AUTHORIZATION, bearer(memberToken)))
				.andReturn().getResponse().getContentAsString();
		List<Number> ids = JsonPath.read(list, "$[?(@.formId == " + formId + ")].id");
		return ids.getFirst().longValue();
	}

	protected ResultActions submit(String memberToken, long reviewId, String answersJson) throws Exception {
		return mvc.perform(post("/api/me/reviews/" + reviewId + "/submit")
				.header(HttpHeaders.AUTHORIZATION, bearer(memberToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"answers\":" + answersJson + "}"));
	}

	protected String validAnswers() {
		return "[{\"questionId\":" + scaleQuestionId + ",\"scaleValue\":4},"
				+ "{\"questionId\":" + textQuestionId + ",\"textValue\":\"Több dokumentáció\"}]";
	}

}
