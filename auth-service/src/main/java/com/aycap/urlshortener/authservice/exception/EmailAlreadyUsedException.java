package com.aycap.urlshortener.authservice.exception;

import com.aycap.urlshortener.authservice.common.response.StatusCode;

public class EmailAlreadyUsedException extends BusinessException {

	public EmailAlreadyUsedException(String email) {
		super(StatusCode.ERR_EMAIL_TAKEN, "Email already registered: " + email);
	}

}
