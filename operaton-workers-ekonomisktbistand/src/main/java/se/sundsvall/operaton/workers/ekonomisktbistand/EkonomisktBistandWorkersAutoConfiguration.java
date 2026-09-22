package se.sundsvall.operaton.workers.ekonomisktbistand;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * The EB process's own workers. The Feign clients and properties they use are contributed by the integration modules'
 * own auto-configurations, so this one only needs the component scan.
 */
@AutoConfiguration
@ComponentScan("se.sundsvall.operaton.workers.ekonomisktbistand")
public class EkonomisktBistandWorkersAutoConfiguration {
}
