package se.sundsvall.operaton.workers.rtjmanagement;

import generated.se.sundsvall.rtjmanagement.Decision;
import generated.se.sundsvall.rtjmanagement.PatchErrand;
import generated.se.sundsvall.rtjmanagement.Stakeholder;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import se.sundsvall.operaton.workers.rtjmanagement.configuration.RtjManagementConfiguration;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.operaton.workers.rtjmanagement.configuration.RtjManagementConfiguration.CLIENT_ID;

/**
 * Workers in this module only enrich errands that were created by rtj-management
 * itself — the citizen/admin POSTs to rtj-management, which then auto-starts the
 * BPMN with the errandId already in process variables. There is therefore no
 * {@code createErrand} method here on purpose.
 */
@FeignClient(
	name = CLIENT_ID,
	url = "${integration.rtj-management.url}",
	configuration = RtjManagementConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface RtjManagementClient {

	@PatchMapping(path = "/{municipalityId}/{namespace}/errands/{errandId}", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<Void> updateErrand(
		@PathVariable final String municipalityId,
		@PathVariable final String namespace,
		@PathVariable final String errandId,
		@RequestBody final PatchErrand patchErrand);

	@PostMapping(path = "/{municipalityId}/{namespace}/errands/{errandId}/decisions", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<Void> createErrandDecision(
		@PathVariable final String municipalityId,
		@PathVariable final String namespace,
		@PathVariable final String errandId,
		@RequestBody final Decision decision);

	@PostMapping(path = "/{municipalityId}/{namespace}/errands/{errandId}/stakeholders", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<Void> createErrandStakeholder(
		@PathVariable final String municipalityId,
		@PathVariable final String namespace,
		@PathVariable final String errandId,
		@RequestBody final Stakeholder stakeholder);
}
