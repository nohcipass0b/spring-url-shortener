package com.aycap.urlshortener.authservice.model.dto;

/**
 * @param token short-lived access token, sent on every request
 * @param refreshToken long-lived, exchanged at /refresh;
 * @param expiresInSeconds lifetime of the access token, not the refresh token
 */
public record AuthResponse(String token, String refreshToken, String tokenType, long expiresInSeconds) {

	public static AuthResponse bearer(String token, String refreshToken, long expiresInSeconds) {
		return new AuthResponse(token, refreshToken, "Bearer", expiresInSeconds);
	}
}
