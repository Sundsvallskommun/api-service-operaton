package se.sundsvall.operaton.deployment.api;

import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import se.sundsvall.operaton.app.Application;
import se.sundsvall.operaton.decision.service.DmnService;
import se.sundsvall.operaton.deployment.api.model.DeploymentResponse;
import se.sundsvall.operaton.deployment.api.model.DeploymentsResponse;
import se.sundsvall.operaton.deployment.service.DeploymentService;
import se.sundsvall.operaton.process.service.ProcessService;
import se.sundsvall.operaton.workers.service.TopicService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class DeploymentResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String DEPLOYMENTS_PATH = "/{municipalityId}/deployments";
	private static final String DEPLOYMENT_PATH = "/{municipalityId}/deployments/{id}";

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
	void deploy() {
		final var deploymentResponse = DeploymentResponse.create()
			.withId("deploy-1")
			.withName("test-deployment")
			.withDeploymentTime(OffsetDateTime.now());

		when(deploymentServiceMock.deploy(eq("test-deployment"), any())).thenReturn(deploymentResponse);

		final var bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.part("name", "test-deployment");
		bodyBuilder.part("file", "bpmn-content".getBytes()).filename("process.bpmn");

		final var response = webTestClient.post()
			.uri(builder -> builder.path(DEPLOYMENTS_PATH).build(Map.of("municipalityId", MUNICIPALITY_ID)))
			.body(BodyInserters.fromMultipartData(bodyBuilder.build()))
			.exchange()
			.expectStatus().isOk()
			.expectBody(DeploymentResponse.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getId()).isEqualTo("deploy-1");
		verify(deploymentServiceMock).deploy(eq("test-deployment"), any());
	}

	@Test
	void getDeployments() {
		final var deploymentsResponse = DeploymentsResponse.create()
			.withDeployments(java.util.List.of(DeploymentResponse.create()
				.withId("deploy-1")
				.withName("test")));

		when(deploymentServiceMock.getDeployments()).thenReturn(deploymentsResponse);

		final var response = webTestClient.get()
			.uri(builder -> builder.path(DEPLOYMENTS_PATH).build(Map.of("municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBody(DeploymentsResponse.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getDeployments()).hasSize(1);
		assertThat(response.getDeployments().getFirst().getId()).isEqualTo("deploy-1");
		verify(deploymentServiceMock).getDeployments();
	}

	@Test
	void deleteDeployment() {
		webTestClient.delete()
			.uri(builder -> builder.path(DEPLOYMENT_PATH)
				.queryParam("cascade", "true")
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "id", "deploy-1")))
			.exchange()
			.expectStatus().isNoContent();

		verify(deploymentServiceMock).deleteDeployment("deploy-1", true);
	}

	@Test
	void deleteDeploymentWithoutCascade() {
		webTestClient.delete()
			.uri(builder -> builder.path(DEPLOYMENT_PATH)
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "id", "deploy-1")))
			.exchange()
			.expectStatus().isNoContent();

		verify(deploymentServiceMock).deleteDeployment("deploy-1", false);
	}
}
