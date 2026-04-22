package se.sundsvall.operaton.workers.caremanagement;

import generated.se.sundsvall.caremanagement.Errand;
import generated.se.sundsvall.caremanagement.PatchErrand;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import se.sundsvall.operaton.workers.caremanagement.configuration.CareManagementConfiguration;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.operaton.workers.caremanagement.configuration.CareManagementConfiguration.CLIENT_ID;

@FeignClient(
	name = CLIENT_ID,
	url = "${integration.care-management.url}",
	configuration = CareManagementConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface CareManagementClient {

	@PostMapping(path = "/{municipalityId}/{namespace}/errands", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<Void> createErrand(
		@PathVariable final String municipalityId,
		@PathVariable final String namespace,
		@RequestBody final Errand errand);

	@PatchMapping(path = "/{municipalityId}/{namespace}/errands/{errandId}", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<Void> updateErrand(
		@PathVariable final String municipalityId,
		@PathVariable final String namespace,
		@PathVariable final String errandId,
		@RequestBody final PatchErrand patchErrand);
}
