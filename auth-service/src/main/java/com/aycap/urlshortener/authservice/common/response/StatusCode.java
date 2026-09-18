package com.aycap.urlshortener.authservice.common.response;


public enum StatusCode {
	SUCCESS("Success"),
	ERR_VALIDATION("Validation failed"),
	ERR_NOT_FOUND("Short URL not found"),
	ERR_UNAUTHORIZED("Authentication required"),
	ERR_INTERNAL("Unexpected error");

	private final String en;

	StatusCode(String en) {
		this.en = en;
	}

	public String en() {
		return en;
	}
}