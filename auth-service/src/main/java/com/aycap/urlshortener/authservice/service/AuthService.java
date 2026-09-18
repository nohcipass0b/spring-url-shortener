package com.aycap.urlshortener.authservice.service;

import com.aycap.urlshortener.authservice.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.aycap.urlshortener.authservice.model.dto.RegisterRequest;
import com.aycap.urlshortener.authservice.model.dto.RegisterResponse;
import com.aycap.urlshortener.authservice.common.exception.EmailAlreadyUsedException;
import com.aycap.urlshortener.authservice.model.entity.User;

import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private final UserRepository userRepository;

	private final PasswordEncoder passwordEncoder;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public RegisterResponse register(RegisterRequest request) {

		if (userRepository.existsByEmail(request.email())) {
			throw new EmailAlreadyUsedException(request.email());
		}

		return RegisterResponse
			.from(userRepository.save(new User(request.email(), passwordEncoder.encode(request.password()))));
	}

}
