package com.aycap.urlshortener.urlservice.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ShortenRequest(

		@JsonProperty("original_url") @NotBlank(message = "original_url is required") @Size(max = 2048, message = "original_url must be at most 2048 characters")
		// only http(s): rejects javascript: and data: URLs, which would otherwise
		// turn every short link into a stored-XSS vector
		@Pattern(regexp = "^https?://.+", message = "original_url must be a valid http or https URL") String originalUrl) {
}
