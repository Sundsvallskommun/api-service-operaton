package se.sundsvall.operaton.workers.caremanagement;

import generated.se.sundsvall.caremanagement.ActualisationRequest;
import generated.se.sundsvall.caremanagement.ActualisationResponse;
import generated.se.sundsvall.caremanagement.Decision;
import generated.se.sundsvall.caremanagement.Errand;
import generated.se.sundsvall.caremanagement.NormberakningRequest;
import generated.se.sundsvall.caremanagement.NormberakningResponse;
import generated.se.sundsvall.caremanagement.Parameter;
import generated.se.sundsvall.caremanagement.PatchErrand;
import generated.se.sundsvall.caremanagement.PaymentStatusRequest;
import generated.se.sundsvall.caremanagement.PaymentStatusResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Map;
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

	@PostMapping(path = "/{municipalityId}/{namespace}/errands/{errandId}/parameters", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<Void> createErrandParameter(
		@PathVariable final String municipalityId,
		@PathVariable final String namespace,
		@PathVariable final String errandId,
		@RequestBody final Parameter parameter);

	@PostMapping(path = "/{municipalityId}/{namespace}/errands/{errandId}/decisions", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<Void> createErrandDecision(
		@PathVariable final String municipalityId,
		@PathVariable final String namespace,
		@PathVariable final String errandId,
		@RequestBody final Decision decision);

	@PostMapping(path = "/{municipalityId}/{namespace}/errands/financial-assistance/calculation/prepare", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<NormberakningResponse> prepareNormberakning(
		@PathVariable final String municipalityId,
		@PathVariable final String namespace,
		@RequestBody final NormberakningRequest request);

	@PostMapping(path = "/{municipalityId}/{namespace}/errands/financial-assistance/calculation/commit", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<NormberakningResponse> commitNormberakning(
		@PathVariable final String municipalityId,
		@PathVariable final String namespace,
		@RequestBody final NormberakningRequest request);

	@PostMapping(path = "/{municipalityId}/{namespace}/errands/financial-assistance/calculation/from-application", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<NormberakningResponse> createApplicationNormberakning(
		@PathVariable final String municipalityId,
		@PathVariable final String namespace,
		@RequestBody final NormberakningRequest request);

	@PostMapping(path = "/{municipalityId}/{namespace}/errands/financial-assistance/actualisation", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<ActualisationResponse> createActualisation(
		@PathVariable final String municipalityId,
		@PathVariable final String namespace,
		@RequestBody final ActualisationRequest request);

	@PostMapping(path = "/{municipalityId}/{namespace}/errands/financial-assistance/payment-status", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<PaymentStatusResponse> checkPaymentStatus(
		@PathVariable final String municipalityId,
		@PathVariable final String namespace,
		@RequestBody final PaymentStatusRequest request);

	/**
	 * Enqueue a UiPath RPA task on an errand (CareManagement drops a queue item; a robot does the Lifecare GUI work out of
	 * band). Body is {@code {"action": "...", "parameters": {...}}}; sent as a Map so no generated DTO is needed.
	 */
	@PostMapping(path = "/{municipalityId}/{namespace}/errands/{errandId}/rpa-tasks", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<Void> enqueueRpaTask(
		@PathVariable final String municipalityId,
		@PathVariable final String namespace,
		@PathVariable final String errandId,
		@RequestBody final Map<String, Object> request);
}
