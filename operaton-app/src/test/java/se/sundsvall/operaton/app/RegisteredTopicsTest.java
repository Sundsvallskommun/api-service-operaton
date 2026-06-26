package se.sundsvall.operaton.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import se.sundsvall.operaton.decision.service.DmnService;
import se.sundsvall.operaton.deployment.service.DeploymentService;
import se.sundsvall.operaton.process.service.ProcessService;
import se.sundsvall.operaton.workers.api.model.TopicDescription;
import se.sundsvall.operaton.workers.framework.TopicRegistry;
import se.sundsvall.operaton.workers.service.TopicService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Guards the full set of external task topics wired into the running application. Booting the real {@link Application}
 * context activates every worker module's auto-configuration, so a worker that silently fails to register — e.g. a
 * module whose AutoConfiguration.imports points at the wrong class — is caught here instead of surfacing as a missing
 * BPMN building block in production. When a worker is added or removed, update the expected set below to match.
 *
 * <p>
 * Mirrors the {@code @SpringBootTest} configuration of the other app tests (same web environment, profile and mocked
 * services) so it shares their cached context. A diverging configuration would boot a second process engine into the
 * JVM-static engine registry and break the suite.
 */
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class RegisteredTopicsTest {

	@MockitoBean
	private DeploymentService deploymentServiceMock;

	@MockitoBean
	private ProcessService processServiceMock;

	@MockitoBean
	private TopicService topicServiceMock;

	@MockitoBean
	private DmnService dmnServiceMock;

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
