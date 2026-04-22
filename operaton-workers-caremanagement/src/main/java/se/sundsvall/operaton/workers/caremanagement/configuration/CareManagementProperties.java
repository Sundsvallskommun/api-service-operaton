package se.sundsvall.operaton.workers.caremanagement.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("integration.care-management")
public record CareManagementProperties(int connectTimeout, int readTimeout) {
}
