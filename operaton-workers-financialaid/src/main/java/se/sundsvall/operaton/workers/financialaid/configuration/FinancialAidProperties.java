package se.sundsvall.operaton.workers.financialaid.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("integration.financial-aid")
public record FinancialAidProperties(int connectTimeout, int readTimeout) {
}
