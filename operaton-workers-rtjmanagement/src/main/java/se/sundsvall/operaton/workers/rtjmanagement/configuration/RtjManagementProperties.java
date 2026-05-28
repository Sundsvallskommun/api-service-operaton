package se.sundsvall.operaton.workers.rtjmanagement.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("integration.rtj-management")
public record RtjManagementProperties(int connectTimeout, int readTimeout) {
}
