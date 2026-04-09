package se.sundsvall.operaton.apptest;

import java.util.List;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.operaton.Application;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.OK;

/**
 * Integration tests for the element-template catalog endpoint. Asserts that every {@code @TopicWorker} registered in
 * the service is surfaced as a bpmn-js element template via
 * {@code GET /{municipalityId}/topics/templates}.
 */
@WireMockAppTestSuite(files = "classpath:/TopicsIT/", classes = Application.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TopicsIT extends AbstractAppTest {

	private static final String PATH = "/2281/topics/templates";
	private static final String RESPONSE_FILE = "response.json";

	@Test
	void test01_listsElementTemplates() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(GET)
			.withJsonAssertOptions(List.of(Option.IGNORING_ARRAY_ORDER, Option.IGNORING_EXTRA_FIELDS))
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}
}
