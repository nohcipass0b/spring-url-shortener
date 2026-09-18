package com.aycap.urlshortener.authservice.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aycap.urlshortener.authservice.common.response.ApiResponse;
import com.aycap.urlshortener.authservice.model.dto.RegisterResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController 
@RequestMapping("/api")
public class AuthController {
    // TODO: will add auth service later after testing controller
    // private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@RequestBody String entity) {
        //TODO: process POST request
        
        RegisterResponse mock = new RegisterResponse("usr_0001", "apichon", "apichon@example.com");

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(mock));
    }
    
    
}
