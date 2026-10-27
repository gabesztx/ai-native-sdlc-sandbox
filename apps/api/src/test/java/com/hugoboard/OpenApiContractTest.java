package com.hugoboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class OpenApiContractTest {

	private static final Path CONTRACT = Path.of("../../contracts/openapi.json");

	@Autowired
	private WebApplicationContext context;

	@Test
	void committedContractMatchesGeneratedOpenApi() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
		String generated = mockMvc.perform(get("/v3/api-docs"))
				.andReturn()
				.getResponse()
				.getContentAsString(StandardCharsets.UTF_8)
				.strip() + "\n";

		if (Boolean.getBoolean("contract.update")) {
			Files.createDirectories(CONTRACT.getParent());
			Files.writeString(CONTRACT, generated);
		}

		assertThat(Files.exists(CONTRACT))
				.as("contracts/openapi.json is missing; run: npm run contract:update")
				.isTrue();
		assertThat(Files.readString(CONTRACT))
				.as("contracts/openapi.json is stale; run: npm run contract:update")
				.isEqualTo(generated);
	}

}
