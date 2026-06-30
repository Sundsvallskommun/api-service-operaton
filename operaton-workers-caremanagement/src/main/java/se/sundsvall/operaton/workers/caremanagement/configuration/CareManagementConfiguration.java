package se.sundsvall.operaton.workers.caremanagement.configuration;

import feign.RequestInterceptor;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import se.sundsvall.dept44.configuration.feign.FeignConfiguration;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.ProblemErrorDecoder;
import se.sundsvall.dept44.support.Identifier;

@Import(FeignConfiguration.class)
public class CareManagementConfiguration {

	public static final String CLIENT_ID = "care-management";

	/**
	 * Identifies these worker calls to care-management as coming from the process engine. care-management reads the
	 * {@code X-Sent-By} header into the actor of its errand event log, so process-driven writes (decisions, status,
	 * normberäkning, ...) are attributed to {@code operaton} instead of an anonymous null actor.
	 */
	static final String SENT_BY = "operaton; type=custom";

	@Bean
	FeignBuilderCustomizer feignBuilderCustomizer(final CareManagementProperties careManagementProperties, final ClientRegistrationRepository clientRegistrationRepository) {
		return FeignMultiCustomizer.create()
			.withErrorDecoder(new ProblemErrorDecoder(CLIENT_ID))
			.withRequestTimeoutsInSeconds(careManagementProperties.connectTimeout(), careManagementProperties.readTimeout())
			.withRetryableOAuth2InterceptorForClientRegistration(clientRegistrationRepository.findByRegistrationId(CLIENT_ID))
			.composeCustomizersToOne();
	}

	@Bean
	RequestInterceptor careManagementSentByInterceptor() {
		return requestTemplate -> requestTemplate.header(Identifier.HEADER_NAME, SENT_BY);
	}
}
