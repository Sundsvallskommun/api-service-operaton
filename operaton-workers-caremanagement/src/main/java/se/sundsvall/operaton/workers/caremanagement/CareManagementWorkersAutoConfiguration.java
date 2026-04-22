package se.sundsvall.operaton.workers.caremanagement;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import se.sundsvall.operaton.workers.caremanagement.configuration.CareManagementProperties;

@AutoConfiguration
@ComponentScan("se.sundsvall.operaton.workers.caremanagement")
@EnableFeignClients(basePackageClasses = CareManagementClient.class)
@EnableConfigurationProperties(CareManagementProperties.class)
public class CareManagementWorkersAutoConfiguration {
}
