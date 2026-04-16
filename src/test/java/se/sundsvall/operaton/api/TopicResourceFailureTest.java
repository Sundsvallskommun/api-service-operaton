package se.sundsvall.operaton.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.operaton.Application;
import se.sundsvall.operaton.service.DeploymentService;
import se.sundsvall.operaton.service.DmnService;
import se.sundsvall.operaton.service.ProcessService;
import se.sundsvall.operaton.service.TopicService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class TopicResourceFailureTest {

	private static final String INVALID_MUNICIPALITY_ID = "bad-municipality-id";
	private static final String TOPICS_PATH = "/{municipalityId}/topics";
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
	void getTopicsWithInvalidMunicipalityId() {
		final var response = webTestClient.get()
			.uri(builder -> builder.path(TOPICS_PATH).build(Map.of("municipalityId", INVALID_MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);

		verifyNoInteractions(topicServiceMock);
	}

	@Test
	void getElementTemplatesWithInvalidMunicipalityId() {
		final var response = webTestClient.get()
			.uri(builder -> builder.path(TEMPLATES_PATH).build(Map.of("municipalityId", INVALID_MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);

		verifyNoInteractions(topicServiceMock);
	}
}
