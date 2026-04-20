package se.sundsvall.operaton.decision.api;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.operaton.app.Application;
import se.sundsvall.operaton.decision.api.model.DecisionDefinitionResponse;
import se.sundsvall.operaton.decision.api.model.DecisionDefinitionsResponse;
import se.sundsvall.operaton.decision.service.DmnService;
import se.sundsvall.operaton.deployment.service.DeploymentService;
import se.sundsvall.operaton.process.service.ProcessService;
import se.sundsvall.operaton.workers.service.TopicService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class DecisionResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
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
	void getDecisionDefinitions() {
		final var decisionDefinitionsResponse = DecisionDefinitionsResponse.create()
			.withDecisionDefinitions(List.of(DecisionDefinitionResponse.create()
				.withId("approve-loan:1:5")
				.withKey("approve-loan")
				.withName("Approve Loan")
				.withVersion(1)));

		when(dmnServiceMock.getDecisionDefinitions()).thenReturn(decisionDefinitionsResponse);

		final var response = webTestClient.get()
			.uri(builder -> builder.path(DECISION_DEFINITIONS_PATH).build(Map.of("municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBody(DecisionDefinitionsResponse.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getDecisionDefinitions()).hasSize(1);
		assertThat(response.getDecisionDefinitions().getFirst().getKey()).isEqualTo("approve-loan");
		verify(dmnServiceMock).getDecisionDefinitions();
	}

	@Test
	void getDecisionDefinition() {
		final var dd = DecisionDefinitionResponse.create()
			.withId("approve-loan:1:5")
			.withKey("approve-loan")
			.withName("Approve Loan")
			.withVersion(1)
			.withDeploymentId("deploy-1");

		when(dmnServiceMock.getDecisionDefinition("approve-loan:1:5")).thenReturn(dd);

		final var response = webTestClient.get()
			.uri(builder -> builder.path(DECISION_DEFINITION_PATH).build(Map.of(
				"municipalityId", MUNICIPALITY_ID,
				"id", "approve-loan:1:5")))
			.exchange()
			.expectStatus().isOk()
			.expectBody(DecisionDefinitionResponse.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getId()).isEqualTo("approve-loan:1:5");
		assertThat(response.getDeploymentId()).isEqualTo("deploy-1");
		verify(dmnServiceMock).getDecisionDefinition("approve-loan:1:5");
	}

	@Test
	void getDecisionDefinitionXml() {
		final var dmnXml = "<dmn>test</dmn>".getBytes();
		when(dmnServiceMock.getDecisionModel("approve-loan:1:5")).thenReturn(dmnXml);

		final var response = webTestClient.get()
			.uri(builder -> builder.path(DECISION_DEFINITION_XML_PATH).build(Map.of(
				"municipalityId", MUNICIPALITY_ID,
				"id", "approve-loan:1:5")))
			.exchange()
			.expectStatus().isOk()
			.expectBody(byte[].class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isEqualTo(dmnXml);
		verify(dmnServiceMock).getDecisionModel("approve-loan:1:5");
	}
}
