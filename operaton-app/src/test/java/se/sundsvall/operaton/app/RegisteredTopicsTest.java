package se.sundsvall.operaton.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.operaton.workers.api.model.TopicDescription;
import se.sundsvall.operaton.workers.framework.TopicRegistry;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the full set of external task topics wired into the running application. Booting the real {@link Application}
 * context activates every worker module's auto-configuration, so a worker that silently fails to register — e.g. a
 * module whose AutoConfiguration.imports points at the wrong class — is caught here instead of surfacing as a missing
 * BPMN building block in production. When a worker is added or removed, update the expected set below to match.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
class RegisteredTopicsTest {

	@Autowired
	private TopicRegistry topicRegistry;

	@Test
	void allExpectedTopicsAreRegistered() {
		final var registeredTopics = topicRegistry.getAll().stream()
			.map(TopicDescription::getTopic)
			.toList();

		assertThat(registeredTopics).containsExactlyInAnyOrder(
			"send-email",
			"send-sms",
			"create-support-errand",
			"create-care-errand",
			"update-errand",
			"update-errand-parameter",
			"add-errand-decision",
			"fetch-financial-aid-basis",
			"log-message");
	}
}
