package se.sundsvall.operaton.workers.financialaid;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import se.sundsvall.operaton.workers.financialaid.configuration.FinancialAidConfiguration;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.operaton.workers.financialaid.configuration.FinancialAidConfiguration.CLIENT_ID;

@FeignClient(
	name = CLIENT_ID,
	url = "${integration.financial-aid.url}",
	configuration = FinancialAidConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface FinancialAidClient {

	@GetMapping(path = "/{municipalityId}/financial-aid", produces = APPLICATION_JSON_VALUE)
	Map<String, Map<String, Object>> getFinancialAidBasis(
		@PathVariable final String municipalityId,
		@RequestParam("personalNumber") final String personalNumber,
		@RequestParam("fromDate") final String fromDate,
		@RequestParam("toDate") final String toDate);
}
