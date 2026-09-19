package com.aycap.urlshortener.urlservice.common.response;

public record ApiStatus(String code, Description description) {

	public static ApiStatus of(StatusCode code) {
		return new ApiStatus(code.name(), new Description(code.en()));
	}
}
