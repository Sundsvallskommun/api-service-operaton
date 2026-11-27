package se.sundsvall.operaton.workers.caremanagement;

import generated.se.sundsvall.caremanagement.ActualisationRequest;
import generated.se.sundsvall.caremanagement.ActualisationResponse;
import generated.se.sundsvall.caremanagement.Decision;
import generated.se.sundsvall.caremanagement.Errand;
import generated.se.sundsvall.caremanagement.HouseholdIdentifiers;
import generated.se.sundsvall.caremanagement.NormberakningRequest;
import generated.se.sundsvall.caremanagement.NormberakningResponse;
import generated.se.sundsvall.caremanagement.Notification;
import generated.se.sundsvall.caremanagement.PatchErrand;
import generated.se.sundsvall.caremanagement.PaymentStatusRequest;
import generated.se.sundsvall.caremanagement.PaymentStatusResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
	 * The household's personal numbers for one errand. Fetched on demand so they never become process variables: the
	 * engine persists every variable in {@code ACT_RU_VARIABLE} and keeps it in {@code ACT_HI_VARINST} for the model's
	 * history TTL, where no gallring reaches it. Carry the errandId, fetch the identities — and every
	 * read lands in the errand's event log.
	 */
	@GetMapping(path = "/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}/household-identifiers", produces = APPLICATION_JSON_VALUE)
	ResponseEntity<HouseholdIdentifiers> getHouseholdIdentifiers(
		@PathVariable final String municipalityId,
		@PathVariable final String namespace,
		@PathVariable final String errandId);

	@GetMapping(path = "/{municipalityId}/{namespace}/errands/{errandId}", produces = APPLICATION_JSON_VALUE)
	ResponseEntity<Errand> readErrand(
		@PathVariable final String municipalityId,
		@PathVariable final String namespace,
		@PathVariable final String errandId);

	@PostMapping(path = "/{municipalityId}/{namespace}/errands/{errandId}/notifications", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<Void> createNotification(
		@PathVariable final String municipalityId,
		@PathVariable final String namespace,
		@PathVariable final String errandId,
		@RequestBody final Notification notification);
}
