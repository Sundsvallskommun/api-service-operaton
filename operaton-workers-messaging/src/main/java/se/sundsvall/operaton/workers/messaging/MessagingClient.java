package se.sundsvall.operaton.workers.messaging;

import generated.se.sundsvall.messaging.EmailRequest;
import generated.se.sundsvall.messaging.MessageResult;
import generated.se.sundsvall.messaging.SmsRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import se.sundsvall.operaton.workers.messaging.configuration.MessagingConfiguration;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.operaton.workers.messaging.configuration.MessagingConfiguration.CLIENT_ID;

@FeignClient(
	name = CLIENT_ID,
	url = "${integration.messaging.url}",
	configuration = MessagingConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface MessagingClient {

	@PostMapping(path = "/{municipalityId}/email", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	MessageResult sendEmail(
		@PathVariable final String municipalityId,
		@RequestBody final EmailRequest request);

	@PostMapping(path = "/{municipalityId}/sms", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	MessageResult sendSms(
		@PathVariable final String municipalityId,
		@RequestBody final SmsRequest request);
}
