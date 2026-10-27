package com.hugoboard.common;

import java.util.Map;

public class BadRequestException extends RuntimeException {

	private final Map<String, String> fieldErrors;

	public BadRequestException(String message, Map<String, String> fieldErrors) {
		super(message);
		this.fieldErrors = Map.copyOf(fieldErrors);
	}

	public Map<String, String> fieldErrors() {
		return fieldErrors;
	}

}
