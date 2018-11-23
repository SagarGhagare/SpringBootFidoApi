/*package com.websystique.springboot.logging;

import static com.websystique.springboot.logging.Level.DEBUG;
import static com.websystique.springboot.logging.Level.ERROR;
import static com.websystique.springboot.logging.Level.INFO;
import static com.websystique.springboot.logging.Level.WARN;

import java.io.PrintStream;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DefaultLogger implements Logger {
	private static ThreadLocal<String> correlationIds = ThreadLocal.withInitial(() -> "");
	private static final String EVENT_KEY = "EVENT";
	private static final String DATA_SEPARATOR = " ";
	private static final String VALUE_SEPARATOR = "=";

	private final PrintStream printStream;
	private final ObjectMapper objectMapper;
	private final String serviceName;
	private final Level logLevel;

	public DefaultLogger(
			final PrintStream printStream,
			final ObjectMapper objectMapper,
			final LogConfiguration config
	) {
		this.printStream = printStream;
		this.objectMapper = objectMapper;
		this.serviceName = config.getServiceName();
		this.logLevel = config.getLogLevel();
	}

	private void log(final Level level, final String message, final Object[] payload) {
		if(level.levelValue < logLevel.levelValue) {
			return;
		}
		final StackTraceElement invocationPoint = Thread.currentThread().getStackTrace()[3];
		final String file = String.format("%s:%s", invocationPoint.getFileName(), invocationPoint.getLineNumber());

		final LogEvent logEvent = new LogEventBuilder()	.withLevel(level)
														.withMessage(message)

														.withFile(file)
														.withClass(invocationPoint.getClassName())
														.withMethod(invocationPoint.getMethodName())

														.withServiceName(serviceName)
														.withPayload(payload)
														.withTime(new Date())
														.withCorrelationId(correlationIds.get())
														.build();

		try {
			printStream.println(objectMapper.writeValueAsString(logEvent));
		} catch(JsonProcessingException e) {
			printStream.println(e.getMessage());
			e.printStackTrace(printStream);
		}
	}

	@Override
	public void setCorrelationId(String correlationId) {
		correlationIds.set(correlationId);
	}

	@Override
	public void unsetCorrelationId() {
		correlationIds.remove();
	}

	@Override
	public void debug(Object... payload) {
		log(DEBUG, null, payload);
	}

	@Override
	public void debug(String message, Object... payload) {
		log(DEBUG, message, payload);
	}

	@Override
	public void info(Object... payload) {
		log(INFO, null, payload);
	}

	@Override
	public void info(String message, Object... payload) {
		log(INFO, message, payload);
	}

	@Override
	public void warn(Object... payload) {
		log(WARN, null, payload);
	}

	@Override
	public void warn(String message, Object... payload) {
		log(WARN, message, payload);
	}

	@Override
	public void error(Object... payload) {
		log(ERROR, null, payload);
	}

	@Override
	public void error(String message, Object... payload) {
		log(ERROR, message, payload);
	}

	public void event(final String event) {
		event(event, Collections.emptyMap());
	}

	public void event(final String event, final Map<String, String> data) {
		StringBuilder messageBuilder = new StringBuilder(EVENT_KEY).append(VALUE_SEPARATOR).append(event);
		if(data != null) {
			for(Map.Entry<String, String> entry : data.entrySet()) {
				messageBuilder	.append(DATA_SEPARATOR)
								.append(entry.getKey())
								.append(VALUE_SEPARATOR)
								.append(entry.getValue());
			}
		}

		this.printStream.println(messageBuilder.toString());
	}
}
*/