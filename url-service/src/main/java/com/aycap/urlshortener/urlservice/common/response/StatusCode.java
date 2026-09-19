package com.aycap.urlshortener.urlservice.common.response;

import org.springframework.http.HttpStatus;

public enum StatusCode {

	SUCCESS("Success", HttpStatus.OK), ERR_VALIDATION("Validation failed", HttpStatus.BAD_REQUEST),
	ERR_NOT_FOUND("Resource not found", HttpStatus.NOT_FOUND),
	ERR_UNAUTHORIZED("Authentication required", HttpStatus.UNAUTHORIZED),
	ERR_INTERNAL("Unexpected error", HttpStatus.INTERNAL_SERVER_ERROR);

	private final String en;

	private final HttpStatus httpStatus;

	StatusCode(String en, HttpStatus httpStatus) {
		this.en = en;
		this.httpStatus = httpStatus;
	}

	public String en() {
		return this.en;
	}

	public HttpStatus httpStatus() {
		return this.httpStatus;
	}

	// a status the framework produced on its own, mapped back onto our codes
	public static StatusCode forHttpStatus(int value) {
		for (StatusCode candidate : values()) {
			if (candidate != SUCCESS && candidate.httpStatus.value() == value) {
				return candidate;
			}
		}
		return ERR_INTERNAL;
	}

}
