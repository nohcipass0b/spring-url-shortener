package com.aycap.urlshortener.urlservice.controller;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.aycap.urlshortener.urlservice.service.ShortUrlService;

@RestController
public class RedirectController {

	private final ShortUrlService service;

	public RedirectController(ShortUrlService service) {
		this.service = service;
	}

	@GetMapping("/r/{code}")
	public ResponseEntity<Void> redirect(@PathVariable String code) {
		return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(service.resolve(code))).build();
	}

}
