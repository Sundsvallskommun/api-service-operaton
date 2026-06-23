package se.sundsvall.operaton.workers.caremanagement.configuration;

import feign.RequestTemplate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CareManagementConfigurationTest {

	@Test
	void sentByInterceptorTagsRequestsAsOperaton() {
		final var template = new RequestTemplate();

		new CareManagementConfiguration().careManagementSentByInterceptor().apply(template);

		assertThat(template.headers().get("X-Sent-By")).containsExactly("operaton; type=custom");
	}
}
