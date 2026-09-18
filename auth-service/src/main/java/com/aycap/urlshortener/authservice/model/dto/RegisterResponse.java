package com.aycap.urlshortener.authservice.model.dto;

import com.aycap.urlshortener.authservice.model.entity.User;
import java.time.Instant;
import java.util.UUID;

/**
 * Returned by /register. Deliberately carries no tokens: registering proves you
 * chose a password, not that you can supply it. The client logs in afterwards.
 */
public record RegisterResponse(UUID id, String email, Instant createdAt) {

	public static RegisterResponse from(User user) {
		return new RegisterResponse(user.getId(), user.getEmail(), user.getCreatedAt());
	}
}