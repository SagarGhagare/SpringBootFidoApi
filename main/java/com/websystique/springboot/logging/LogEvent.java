package com.websystique.springboot.logging;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class LogEvent {
	public final String file;
	@JsonProperty(value = "class")
	public final String clazz;
	public final String method;
	public final String serviceName;
	@JsonFormat(shape = JsonFormat.Shape.STRING,
				pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
				timezone = "UTC")
	public final Date time;
	public final Level level;
	public final String message;
	public final Object[] payload;
	public final String correlationId;

	public LogEvent(
			String file, String clazz, String method, String serviceName, Date time, Level level, String message, Object[] payload, String correlationId) {
		this.file = file;
		this.clazz = clazz;
		this.method = method;
		this.serviceName = serviceName;
		this.time = time;
		this.level = level;
		this.message = message;
		this.payload = payload;
		this.correlationId = correlationId;
	}
}
