package se.sundsvall.operaton.workers;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;

/**
 * Scans the worker framework + topic catalog beans in this module, but excludes the
 * {@code workers.messaging} and {@code workers.supportmanagement} subpackages so those
 * concrete-worker modules are only activated when their own AutoConfigurations are on
 * the classpath.
 */
@AutoConfiguration
@ComponentScan(
	basePackages = "se.sundsvall.operaton.workers",
	excludeFilters = @Filter(
		type = FilterType.REGEX,
		pattern = "se\\.sundsvall\\.operaton\\.workers\\.(messaging|supportmanagement)\\..*"))
public class WorkersAutoConfiguration {
}
