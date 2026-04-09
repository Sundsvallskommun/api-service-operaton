package se.sundsvall.operaton.integration.supportmanagement;

import generated.se.sundsvall.supportmanagement.Errand;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import se.sundsvall.operaton.integration.supportmanagement.configuration.SupportManagementConfiguration;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.operaton.integration.supportmanagement.configuration.SupportManagementConfiguration.CLIENT_ID;

@FeignClient(
	name = CLIENT_ID,
	url = "${integration.support-management.url}",
	configuration = SupportManagementConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface SupportManagementClient {

	@PostMapping(path = "/{municipalityId}/{namespace}/errands", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<Void> createErrand(
		@PathVariable("municipalityId") final String municipalityId,
		@PathVariable("namespace") final String namespace,
		@RequestBody final Errand errand);
}
