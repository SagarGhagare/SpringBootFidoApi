package com.websystique.springboot.logging;

import java.util.Date;

final class LogEventBuilder {
	private Level level;
	private String message;
	private String file;
	private String clazz;
	private String method;
	private String serviceName;
	private Date time;
	private Object[] payload;
	private String correlationId;

	LogEventBuilder withFile(String file) {
		this.file = file;
		return this;
	}

	LogEventBuilder withClass(String clazz) {
		this.clazz = clazz;
		return this;
	}

	LogEventBuilder withMethod(String method) {
		this.method = method;
		return this;
	}

	LogEventBuilder withServiceName(String serviceName) {
		this.serviceName = serviceName;
		return this;
	}

	LogEventBuilder withLevel(Level level) {
		this.level = level;
		return this;
	}

	LogEventBuilder withMessage(String message) {
		this.message = message;
		return this;
	}

	LogEventBuilder withTime(Date time) {
		this.time = time;
		return this;
	}

	LogEventBuilder withPayload(Object[] payload) {
		this.payload = payload;
		return this;
	}

	LogEventBuilder withCorrelationId(String correlationId) {
		this.correlationId = correlationId;
		return this;
	}

	public LogEvent build() {
		return new LogEvent(file, clazz, method, serviceName, time, level, message, payload, correlationId);
	}
}
