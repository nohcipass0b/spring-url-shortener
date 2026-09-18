package com.aycap.urlshortener.authservice.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aycap.urlshortener.authservice.common.response.ApiResponse;
import com.aycap.urlshortener.authservice.model.dto.RegisterRequest;
import com.aycap.urlshortener.authservice.model.dto.RegisterResponse;
import com.aycap.urlshortener.authservice.service.AuthService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

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
    
    
}
