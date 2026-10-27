package com.hugoboard.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hugoboard.support.ApiTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class MemberLoginApiTest extends ApiTestSupport {

	@Test
	@DisplayName("AC5: létező csapattaggal a belépés sikeres, MEMBER szerepkör és azonosító")
	void memberLogsIn() throws Exception {
		long memberId = insertMember("Teszt Elek");

		mvc.perform(post("/api/auth/member/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"memberId\":" + memberId + "}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.me.role").value("MEMBER"))
				.andExpect(jsonPath("$.me.memberId").value(memberId))
				.andExpect(jsonPath("$.me.name").value("Teszt Elek"));
	}

	@Test
	@DisplayName("AC6: nem létező csapattag-azonosítóval 404")
	void unknownMemberIsNotFound() throws Exception {
		mvc.perform(post("/api/auth/member/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"memberId\":999999}"))
				.andExpect(status().isNotFound());
	}

}
