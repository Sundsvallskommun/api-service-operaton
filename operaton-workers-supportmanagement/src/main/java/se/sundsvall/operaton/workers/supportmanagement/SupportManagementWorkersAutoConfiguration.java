package se.sundsvall.operaton.workers.supportmanagement;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import se.sundsvall.operaton.workers.supportmanagement.configuration.SupportManagementProperties;

@AutoConfiguration
@ComponentScan("se.sundsvall.operaton.workers.supportmanagement")
@EnableFeignClients(basePackageClasses = SupportManagementClient.class)
@EnableConfigurationProperties(SupportManagementProperties.class)
public class SupportManagementWorkersAutoConfiguration {
}
