package com.hugoboard.user;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hugoboard.support.ApiTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TeamMemberApiTest extends ApiTestSupport {

	@Test
	@DisplayName("AC1: a csapattagok listája belépés nélkül lekérhető, az admin nincs benne")
	void listsTeamMembersWithoutLogin() throws Exception {
		insertMember("Teszt Elek");
		insertMember("Minta Anna");

		mvc.perform(get("/api/auth/team-members"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[*].name", containsInAnyOrder("Teszt Elek", "Minta Anna")))
				.andExpect(jsonPath("$[0].id").isNumber());
	}

	@Test
	@DisplayName("AC15: csak az aktív csapattagok jelennek meg, csak id és name mezővel")
	void onlyActiveMembersAreListed() throws Exception {
		insertMember("Teszt Elek", true);
		insertMember("Inaktív Ilona", false);

		mvc.perform(get("/api/auth/team-members"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].name").value("Teszt Elek"))
				.andExpect(jsonPath("$[0].id").isNumber());

		mvc.perform(get("/api/admin/team-members").header("Authorization", bearer(adminToken())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].name").value("Teszt Elek"));
	}

}
