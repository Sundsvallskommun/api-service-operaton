package se.sundsvall.operaton.workers.framework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a Spring bean as an external task worker and provides metadata for the topic catalog. Workers annotated with
 * this will be automatically registered in the TopicRegistry, making them discoverable via the /topics endpoint.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TopicWorker {

	/**
	 * The external task topic name.
	 */
	String topic();

	/**
	 * Human-readable description of what this worker does.
	 */
	String description() default "";

	/**
	 * Input variable names expected by this worker.
	 */
	String[] inputVariables() default {};

	/**
	 * Output variable names set by this worker.
	 */
	String[] outputVariables() default {};
}
