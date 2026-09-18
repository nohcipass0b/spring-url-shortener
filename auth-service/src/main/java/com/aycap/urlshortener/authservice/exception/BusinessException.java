package com.aycap.urlshortener.authservice.exception;

import com.aycap.urlshortener.authservice.common.response.StatusCode;

public class BusinessException extends RuntimeException {

	private final StatusCode code;

	protected BusinessException(StatusCode code, String message) {
		super(message);
		this.code = code;
	}

	public StatusCode code() {
		return code;
	}

}
