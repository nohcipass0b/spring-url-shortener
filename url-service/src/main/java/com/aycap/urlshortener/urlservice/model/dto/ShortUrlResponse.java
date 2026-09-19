package com.aycap.urlshortener.urlservice.model.dto;

import java.time.Instant;
import java.util.UUID;

import com.aycap.urlshortener.urlservice.model.entity.ShortUrl;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ShortUrlResponse(UUID id, String code, @JsonProperty("short_url") String shortUrl,
		@JsonProperty("original_url") String originalUrl, boolean active, @JsonProperty("click_count") long clickCount,
		@JsonProperty("created_at") Instant createdAt) {

	public static ShortUrlResponse from(ShortUrl url, String shortUrl) {
		return new ShortUrlResponse(url.getId(), url.getCode(), shortUrl, url.getOriginalUrl(), url.isActive(),
				url.getClickCount(), url.getCreatedAt());
	}
}
