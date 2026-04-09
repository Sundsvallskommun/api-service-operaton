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
import se.sundsvall.operaton.api.model.ProcessDefinitionResponse;
import se.sundsvall.operaton.api.model.ProcessInstanceResponse;
import se.sundsvall.operaton.api.model.StartProcessInstanceRequest;
import se.sundsvall.operaton.service.DeploymentService;
import se.sundsvall.operaton.service.ProcessService;
import se.sundsvall.operaton.service.TopicService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class ProcessResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String PROCESS_DEFINITIONS_PATH = "/{municipalityId}/process-definitions";
	private static final String PROCESS_INSTANCES_PATH = "/{municipalityId}/process-instances";
	private static final String PROCESS_INSTANCE_PATH = "/{municipalityId}/process-instances/{id}";

	@MockitoBean
	private DeploymentService deploymentServiceMock;

	@MockitoBean
	private ProcessService processServiceMock;

	@MockitoBean
	private TopicService topicServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void getProcessDefinitions() {
		final var pd = ProcessDefinitionResponse.create()
			.withId("invoice:1:4")
			.withKey("invoice")
			.withName("Invoice Process")
			.withVersion(1);

		when(processServiceMock.getProcessDefinitions()).thenReturn(List.of(pd));

		final var response = webTestClient.get()
			.uri(builder -> builder.path(PROCESS_DEFINITIONS_PATH).build(Map.of("municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(ProcessDefinitionResponse.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).hasSize(1);
		assertThat(response.getFirst().getKey()).isEqualTo("invoice");
		verify(processServiceMock).getProcessDefinitions();
	}

	@Test
	void startProcessInstance() {
		final var piResponse = ProcessInstanceResponse.create()
			.withId("pi-1")
			.withProcessDefinitionId("invoice:1:4");

		when(processServiceMock.startProcessInstance(any())).thenReturn(piResponse);

		final var request = StartProcessInstanceRequest.create()
			.withProcessDefinitionKey("invoice")
			.withBusinessKey("order-123");

		final var response = webTestClient.post()
			.uri(builder -> builder.path(PROCESS_INSTANCES_PATH).build(Map.of("municipalityId", MUNICIPALITY_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(request)
			.exchange()
			.expectStatus().isOk()
			.expectBody(ProcessInstanceResponse.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getId()).isEqualTo("pi-1");
		verify(processServiceMock).startProcessInstance(any());
	}

	@Test
	void getProcessInstances() {
		final var pi = ProcessInstanceResponse.create().withId("pi-1");

		when(processServiceMock.getProcessInstances()).thenReturn(List.of(pi));

		final var response = webTestClient.get()
			.uri(builder -> builder.path(PROCESS_INSTANCES_PATH).build(Map.of("municipalityId", MUNICIPALITY_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(ProcessInstanceResponse.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).hasSize(1);
		verify(processServiceMock).getProcessInstances();
	}

	@Test
	void getProcessInstance() {
		final var pi = ProcessInstanceResponse.create().withId("pi-1");

		when(processServiceMock.getProcessInstance("pi-1")).thenReturn(pi);

		final var response = webTestClient.get()
			.uri(builder -> builder.path(PROCESS_INSTANCE_PATH).build(Map.of(
				"municipalityId", MUNICIPALITY_ID,
				"id", "pi-1")))
			.exchange()
			.expectStatus().isOk()
			.expectBody(ProcessInstanceResponse.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getId()).isEqualTo("pi-1");
		verify(processServiceMock).getProcessInstance("pi-1");
	}
}
