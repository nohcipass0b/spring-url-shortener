package com.aycap.urlshortener.urlservice.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ShortenResponse(@JsonProperty("short_url") String shortUrl) {
}
