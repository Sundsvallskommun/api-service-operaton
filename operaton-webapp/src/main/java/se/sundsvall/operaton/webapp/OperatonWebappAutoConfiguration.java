package se.sundsvall.operaton.webapp;

import org.springframework.boot.autoconfigure.AutoConfiguration;

/**
 * Marker auto-configuration that signals the Operaton Cockpit / Tasklist / Admin
 * webapps should be active. The Operaton webapp starter wires itself entirely
 * via its own auto-configuration when present on the classpath — this class
 * only exists so the module participates in dept44's per-module AutoConfiguration
 * discovery pattern and shows up in {@code Positive matches} on startup.
 */
@AutoConfiguration
public class OperatonWebappAutoConfiguration {
}
