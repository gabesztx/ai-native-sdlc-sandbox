package com.hugoboard.jira;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class JiraConfig {

	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

	private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

	/**
	 * A single, named, singleton builder so {@link JiraClient} and a test's
	 * {@code MockRestServiceServer} always share the same instance.
	 */
	@Bean
	public RestClient.Builder jiraRestClientBuilder() {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
		requestFactory.setReadTimeout(READ_TIMEOUT);
		return RestClient.builder().requestFactory(requestFactory);
	}

}
