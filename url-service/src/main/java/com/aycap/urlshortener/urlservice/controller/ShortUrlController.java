package com.aycap.urlshortener.urlservice.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aycap.urlshortener.urlservice.common.response.ApiResponse;
import com.aycap.urlshortener.urlservice.model.dto.ShortUrlResponse;
import com.aycap.urlshortener.urlservice.model.dto.ShortenRequest;
import com.aycap.urlshortener.urlservice.model.dto.ShortenResponse;
import com.aycap.urlshortener.urlservice.model.entity.ShortUrl;
import com.aycap.urlshortener.urlservice.security.CurrentUser;
// import com.aycap.urlshortener.urlservice.security.CurrentUser;
import com.aycap.urlshortener.urlservice.service.ShortUrlService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class ShortUrlController {

	private final ShortUrlService service;

	public ShortUrlController(ShortUrlService service) {
		this.service = service;
	}

	@PostMapping("/shorten")
	public ResponseEntity<ApiResponse<ShortenResponse>> shorten(@Valid @RequestBody ShortenRequest request) {
		ShortUrl created = service.shorten(request.originalUrl(), CurrentUser.id());
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success(new ShortenResponse(service.shortUrlFor(created))));
	}

	@GetMapping("/urls")
	public ResponseEntity<ApiResponse<List<ShortUrlResponse>>> list() {
		List<ShortUrlResponse> urls = service.listOwnedBy(CurrentUser.id()).stream()
				.map(url -> ShortUrlResponse.from(url, service.shortUrlFor(url))).toList();
		return ResponseEntity.ok(ApiResponse.success(urls));
	}

	@DeleteMapping("/urls/{id}")
	public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
		service.deactivate(id, CurrentUser.id());
		return ResponseEntity.noContent().build();
	}
}
