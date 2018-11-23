package com.websystique.springboot.logging;

public enum Level {
	ERROR(3),
	WARN(2),
	INFO(1),
	DEBUG(0);

	public final Integer levelValue;

	Level(Integer levelValue) {
		this.levelValue = levelValue;
	}
}
