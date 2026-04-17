package se.sundsvall.operaton.apptest;

import java.io.FileNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.operaton.Application;

import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

/**
 * Integration tests for {@code POST /{municipalityId}/deployments}. Locks in the round-trip contract between the BPMN
 * editor (web-operaton-modeler) and the deployment API: a template-generated BPMN file with the expected set of
 * camunda:inputParameter entries must be accepted by Operaton's RepositoryService.
 */
@WireMockAppTestSuite(files = "classpath:/DeploymentIT/", classes = Application.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DeploymentIT extends AbstractAppTest {

	private static final String PATH = "/2281/deployments?name=send-email-process-it";
	private static final String BPMN_FILE = "process.bpmn";
	private static final String RESPONSE_FILE = "response.json";

	@Test
	void test01_deploysTemplateGeneratedBpmn() throws FileNotFoundException {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withContentType(MULTIPART_FORM_DATA)
			.withRequestFile("file", BPMN_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_deploysInvalidBpmn() throws FileNotFoundException {
		setupCall()
			.withServicePath("/2281/deployments?name=broken")
			.withHttpMethod(POST)
			.withContentType(MULTIPART_FORM_DATA)
			.withRequestFile("file", BPMN_FILE)
			.withExpectedResponseStatus(INTERNAL_SERVER_ERROR)
			.sendRequestAndVerifyResponse();
	}
}
