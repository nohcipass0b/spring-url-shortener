package com.aycap.urlshortener.urlservice.exception;

import com.aycap.urlshortener.urlservice.common.response.StatusCode;

public class BusinessException extends RuntimeException {

	private final StatusCode code;

	protected BusinessException(StatusCode code, String message) {
		super(message);
		this.code = code;
	}

	public StatusCode code() {
		return this.code;
	}

}
