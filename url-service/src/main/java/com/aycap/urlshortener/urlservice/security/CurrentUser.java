package com.aycap.urlshortener.urlservice.security;

import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {

	private CurrentUser() {
	}

	public static UUID id() {
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		return (UUID) principal;
	}
}
