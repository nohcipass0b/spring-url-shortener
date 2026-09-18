package com.aycap.urlshortener.authservice.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aycap.urlshortener.authservice.common.response.ApiResponse;
import com.aycap.urlshortener.authservice.model.dto.AuthResponse;
import com.aycap.urlshortener.authservice.model.dto.LoginRequest;
import com.aycap.urlshortener.authservice.model.dto.RegisterRequest;
import com.aycap.urlshortener.authservice.model.dto.RegisterResponse;
import com.aycap.urlshortener.authservice.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(authService.register(request)));
	}

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request,
			HttpServletRequest http) {
		return ResponseEntity.ok(ApiResponse.success(authService.login(request, device(http), ipAddress(http))));
	}

	private String device(HttpServletRequest http) {
		String userAgent = http.getHeader(HttpHeaders.USER_AGENT);
		if (userAgent == null || userAgent.isBlank()) {
			return null;
		}
		return (userAgent.length() > 255) ? userAgent.substring(0, 255) : userAgent;
	}

	private String ipAddress(HttpServletRequest http) {
		String forwardedFor = http.getHeader("X-Forwarded-For");
		if (forwardedFor != null && !forwardedFor.isBlank()) {
			return forwardedFor.split(",")[0].trim();
		}
		return http.getRemoteAddr();
	}

}
