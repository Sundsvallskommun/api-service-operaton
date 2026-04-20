package se.sundsvall.operaton.core;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Development-only CORS configuration that lets a local BPMN editor SPA (e.g. {@code web-operaton-modeler} running on
 * {@code http://localhost:3000}) call this service directly.
 *
 * <p>
 * <strong>Why a {@link WebMvcConfigurer} instead of {@code spring.web.cors.*} properties?</strong>
 * <br>
 * dept44's {@code SecurityConfiguration} installs a {@code SecurityFilterChain} at {@code @Order(0)} that does NOT call
 * {@code http.cors()}. As a result, any {@code CorsConfigurationSource} bean produced by Spring Boot's
 * {@code spring.web.cors.*} auto-configuration is silently ignored. Configuring CORS via {@link WebMvcConfigurer}
 * attaches it at the DispatcherServlet layer, which runs downstream of Spring Security. Because dept44's chain uses
 * {@code anyRequest().permitAll()}, preflight {@code OPTIONS} requests pass through and reach this configuration.
 * </p>
 *
 * <p>
 * This bean is only instantiated when {@code operaton.cors.allowed-origins} is set (typically in
 * {@code application-default.yml} for local dev). In production the service is deployed behind WSO2, which handles
 * CORS via {@code x-wso2-cors} in the OpenAPI spec, so this property is left empty and the bean stays absent.
 * </p>
 */
@Configuration
@ConditionalOnExpression("'${operaton.cors.allowed-origins:}'.length() > 0")
public class WebCorsConfiguration implements WebMvcConfigurer {

	private final String[] allowedOrigins;

	public WebCorsConfiguration(@Value("${operaton.cors.allowed-origins}") final String allowedOrigins) {
		// Plain comma-split + trim — avoids the `\s*,\s*` regex that some
		// static analysers flag as ReDoS-prone (it isn't — two bounded
		// quantifiers around a literal comma run in linear time — but the
		// non-regex form is simpler and sidesteps the warning permanently).
		this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
			.map(String::trim)
			.filter(s -> !s.isEmpty())
			.toArray(String[]::new);
	}

	@Override
	public void addCorsMappings(final CorsRegistry registry) {
		registry.addMapping("/**")
			.allowedOrigins(allowedOrigins)
			.allowedMethods("GET", "POST", "DELETE", "OPTIONS")
			.allowedHeaders("Content-Type", "Authorization", "X-Requested-With")
			.allowCredentials(true);
	}
}
