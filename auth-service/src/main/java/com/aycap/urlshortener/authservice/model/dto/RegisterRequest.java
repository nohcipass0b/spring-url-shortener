package com.aycap.urlshortener.authservice.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

		@NotBlank(message = "email is required") @Email(message = "must be a valid email address") String email,

		// BCrypt silently ignores anything past 72 bytes, so cap it here
		@NotBlank(message = "password is required") @Size(min = 8, max = 72,
				message = "password must be between 8 and 72 characters") String password) {
}
