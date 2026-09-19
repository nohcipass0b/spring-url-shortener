package com.aycap.urlshortener.urlservice.exception;

import com.aycap.urlshortener.urlservice.common.response.StatusCode;

public class ShortUrlNotFoundException extends BusinessException {

	public ShortUrlNotFoundException() {
		super(StatusCode.ERR_NOT_FOUND, "Not found");
	}

}
