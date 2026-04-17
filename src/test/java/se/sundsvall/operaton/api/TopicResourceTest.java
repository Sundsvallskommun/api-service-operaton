package se.sundsvall.operaton.api;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.operaton.Application;
import se.sundsvall.operaton.api.model.ElementTemplate;
import se.sundsvall.operaton.api.model.TopicDescription;
import se.sundsvall.operaton.service.DeploymentService;
import se.sundsvall.operaton.service.DmnService;
import se.sundsvall.operaton.service.ProcessService;
import se.sundsvall.operaton.service.TopicService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class TopicResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String TOPICS_PATH = "/{municipalityId}/topics";
	private static final String TOPIC_PATH = "/{municipalityId}/topics/{topic}";
	private static final String TEMPLATES_PATH = "/{municipalityId}/topics/templates";

	@MockitoBean
	private DeploymentService deploymentServiceMock;

	@MockitoBean
	private ProcessService processServiceMock;

	@MockitoBean
	private TopicService topicServiceMock;

	@MockitoBean
	private DmnService dmnServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void getTopics() {
		final var topicDescription = TopicDescription.create()
			.withTopic("send-email")
			.withDescription("Sends an email");

		when(topicServiceMock.getTopics()).thenReturn(List.of(topicDescription));

		final var response = webTestClient.get()
			.uri(builder -> builder.path(TOPICS_PATH).build(Map.of("municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(TopicDescription.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).hasSize(1);
		assertThat(response.getFirst().getTopic()).isEqualTo("send-email");
		verify(topicServiceMock).getTopics();
	}

	@Test
	void getTopic() {
		final var topicDescription = TopicDescription.create()
			.withTopic("send-email")
			.withDescription("Sends an email");

		when(topicServiceMock.getTopic("send-email")).thenReturn(topicDescription);

		final var response = webTestClient.get()
			.uri(builder -> builder.path(TOPIC_PATH).build(Map.of(
				"municipalityId", MUNICIPALITY_ID,
				"topic", "send-email")))
			.exchange()
			.expectStatus().isOk()
			.expectBody(TopicDescription.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTopic()).isEqualTo("send-email");
		verify(topicServiceMock).getTopic("send-email");
	}

	@Test
	void getElementTemplates() {
		final var template = ElementTemplate.create()
			.withId("se.sundsvall.operaton.SendEmail")
			.withName("Send Email")
			.withSchema("https://unpkg.com/@camunda/element-templates-json-schema/resources/schema.json")
			.withAppliesTo(List.of("bpmn:ServiceTask"));

		when(topicServiceMock.getElementTemplates()).thenReturn(List.of(template));

		final var response = webTestClient.get()
			.uri(builder -> builder.path(TEMPLATES_PATH).build(Map.of("municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(ElementTemplate.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).hasSize(1);
		assertThat(response.getFirst().getId()).isEqualTo("se.sundsvall.operaton.SendEmail");
		assertThat(response.getFirst().getName()).isEqualTo("Send Email");
		verify(topicServiceMock).getElementTemplates();
	}
}
