package se.sundsvall.operaton.workers.rtjmanagement;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import se.sundsvall.operaton.workers.rtjmanagement.configuration.RtjManagementProperties;

@AutoConfiguration
@ComponentScan("se.sundsvall.operaton.workers.rtjmanagement")
@EnableFeignClients(basePackageClasses = RtjManagementClient.class)
@EnableConfigurationProperties(RtjManagementProperties.class)
public class RtjManagementWorkersAutoConfiguration {
}
