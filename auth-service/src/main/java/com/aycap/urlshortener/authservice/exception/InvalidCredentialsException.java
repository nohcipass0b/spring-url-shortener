package com.aycap.urlshortener.authservice.exception;

import com.aycap.urlshortener.authservice.common.response.StatusCode;

public class InvalidCredentialsException extends BusinessException {

	public InvalidCredentialsException() {
		super(StatusCode.ERR_UNAUTHORIZED, "Invalid email or password");
	}

}
