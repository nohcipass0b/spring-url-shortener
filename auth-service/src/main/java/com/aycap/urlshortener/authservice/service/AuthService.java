package com.aycap.urlshortener.authservice.service;

import org.springframework.stereotype.Service;

import com.aycap.urlshortener.authservice.model.dto.RegisterResponse;

import jakarta.transaction.Transactional;

@Service 
public class AuthService {
    // inject user repository ?
    public AuthService(){

    }
   @Transactional 
   public RegisterResponse register(String request) {
     return new RegisterResponse("usr_0001", "apichon", "apichon@example.com");
   }
}
