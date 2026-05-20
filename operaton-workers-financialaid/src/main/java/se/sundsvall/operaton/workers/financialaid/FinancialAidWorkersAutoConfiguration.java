package se.sundsvall.operaton.workers.financialaid;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import se.sundsvall.operaton.workers.financialaid.configuration.FinancialAidProperties;

@AutoConfiguration
@ComponentScan("se.sundsvall.operaton.workers.financialaid")
@EnableFeignClients(basePackageClasses = FinancialAidClient.class)
@EnableConfigurationProperties(FinancialAidProperties.class)
public class FinancialAidWorkersAutoConfiguration {
}
