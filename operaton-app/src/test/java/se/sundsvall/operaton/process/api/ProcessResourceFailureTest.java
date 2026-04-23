package se.sundsvall.operaton.process.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.operaton.app.Application;
import se.sundsvall.operaton.decision.service.DmnService;
import se.sundsvall.operaton.deployment.service.DeploymentService;
import se.sundsvall.operaton.process.api.model.StartProcessInstanceRequest;
import se.sundsvall.operaton.process.service.ProcessService;
import se.sundsvall.operaton.workers.service.TopicService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class ProcessResourceFailureTest {

	private static final String INVALID_MUNICIPALITY_ID = "bad-municipality-id";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String PROCESS_DEFINITIONS_PATH = "/{municipalityId}/process-definitions";
	private static final String PROCESS_INSTANCES_PATH = "/{municipalityId}/process-instances";
	private static final String PROCESS_INSTANCE_VARIABLES_PATH = "/{municipalityId}/process-instances/{id}/variables";

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
	void getProcessDefinitionsWithInvalidMunicipalityId() {
		final var response = webTestClient.get()
			.uri(builder -> builder.path(PROCESS_DEFINITIONS_PATH).build(Map.of("municipalityId", INVALID_MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);

		verifyNoInteractions(processServiceMock);
	}

	@Test
	void startProcessInstanceWithBlankKey() {
		final var request = StartProcessInstanceRequest.create()
			.withProcessDefinitionKey("");

		final var response = webTestClient.post()
			.uri(builder -> builder.path(PROCESS_INSTANCES_PATH).build(Map.of("municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(request)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");

		verifyNoInteractions(processServiceMock);
	}

	@Test
	void modifyProcessInstanceVariablesWithInvalidMunicipalityId() {
		final var response = webTestClient.post()
			.uri(builder -> builder.path(PROCESS_INSTANCE_VARIABLES_PATH).build(Map.of(
				"municipalityId", INVALID_MUNICIPALITY_ID,
				"id", "pi-1")))
			.contentType(APPLICATION_JSON)
			.bodyValue(se.sundsvall.operaton.process.api.model.ModifyVariablesRequest.create())
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");

		verifyNoInteractions(processServiceMock);
	}
}
