package com.aycap.urlshortener.authservice.common.response;

import org.springframework.http.HttpStatus;

public enum StatusCode {
	SUCCESS("Success", HttpStatus.OK),
	ERR_VALIDATION("Validation failed", HttpStatus.BAD_REQUEST),
	ERR_EMAIL_TAKEN("Email already registered", HttpStatus.CONFLICT),
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
		return en;
	}

	public HttpStatus httpStatus() {
		return httpStatus;
	}
}
