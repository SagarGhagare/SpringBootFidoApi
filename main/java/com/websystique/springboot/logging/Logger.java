package com.websystique.springboot.logging;

import java.util.Map;

public interface Logger {

	/**
	 * Sets the correlation ID which will be referenced in any log outputs from the current thread.
	 * @implNote Ensure {@link Logger#unsetCorrelationId() unsetCorrelationId()} is called in a finally block to prevent build-up of correlation IDs in heap (potential memory leak).
	 * @param correlationId The correlation ID for this thread.
	 */
	void setCorrelationId(String correlationId);

	/**
	 * Clean-up to identify correlation ids which are no longer relevant. This should be done to reduce build-up of old data.
	 */
	void unsetCorrelationId();

	void debug(Object... payload);

	void debug(String message, Object... payload);

	void info(Object... payload);

	void info(String message, Object... payload);

	void warn(Object... payload);

	void warn(String message, Object... payload);

	void error(Object... payload);

	void error(String message, Object... payload);

	/**
	 * Logs a event related event given a specific event identifier.
	 * @param event The event identifier.
	 */
	void event(String event);

	/**
	 * Logs a event related event given a specific event identifier and a key-value map of other related data.
	 * @param event The event identifier.
	 * @param data The key-value map of event related data.
	 */
	void event(String event, Map<String, String> data);
}
