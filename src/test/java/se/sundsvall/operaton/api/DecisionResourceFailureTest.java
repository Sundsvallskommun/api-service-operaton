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
class DecisionResourceFailureTest {

	private static final String INVALID_MUNICIPALITY_ID = "bad-municipality-id";
	private static final String DECISION_DEFINITIONS_PATH = "/{municipalityId}/decision-definitions";
	private static final String DECISION_DEFINITION_PATH = "/{municipalityId}/decision-definitions/{id}";
	private static final String DECISION_DEFINITION_XML_PATH = "/{municipalityId}/decision-definitions/{id}/xml";

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
	void getDecisionDefinitionsWithInvalidMunicipalityId() {
		final var response = webTestClient.get()
			.uri(builder -> builder.path(DECISION_DEFINITIONS_PATH).build(Map.of("municipalityId", INVALID_MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);

		verifyNoInteractions(dmnServiceMock);
	}

	@Test
	void getDecisionDefinitionWithInvalidMunicipalityId() {
		final var response = webTestClient.get()
			.uri(builder -> builder.path(DECISION_DEFINITION_PATH).build(Map.of(
				"municipalityId", INVALID_MUNICIPALITY_ID,
				"id", "approve-loan:1:5")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");

		verifyNoInteractions(dmnServiceMock);
	}

	@Test
	void getDecisionDefinitionXmlWithInvalidMunicipalityId() {
		final var response = webTestClient.get()
			.uri(builder -> builder.path(DECISION_DEFINITION_XML_PATH).build(Map.of(
				"municipalityId", INVALID_MUNICIPALITY_ID,
				"id", "approve-loan:1:5")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");

		verifyNoInteractions(dmnServiceMock);
	}
}
