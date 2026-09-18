package com.aycap.urlshortener.authservice.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aycap.urlshortener.authservice.exception.EmailAlreadyUsedException;
import com.aycap.urlshortener.authservice.exception.InvalidCredentialsException;
import com.aycap.urlshortener.authservice.model.dto.AuthResponse;
import com.aycap.urlshortener.authservice.model.dto.LoginRequest;
import com.aycap.urlshortener.authservice.model.dto.RegisterRequest;
import com.aycap.urlshortener.authservice.model.dto.RegisterResponse;
import com.aycap.urlshortener.authservice.model.entity.User;
import com.aycap.urlshortener.authservice.repository.UserRepository;

@Service
public class AuthService {

	private final UserRepository userRepository;

	private final PasswordEncoder passwordEncoder;

	private final JwtService jwtService;

	private final SessionService sessionService;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
			SessionService sessionService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.sessionService = sessionService;
	}

	@Transactional
	public RegisterResponse register(RegisterRequest request) {

		if (userRepository.existsByEmail(request.email())) {
			throw new EmailAlreadyUsedException(request.email());
		}

		return RegisterResponse
				.from(userRepository.save(new User(request.email(), passwordEncoder.encode(request.password()))));
	}

	@Transactional
	public AuthResponse login(LoginRequest request, String device, String ipAddress) {
		User user = userRepository.findByEmail(request.email()).orElseThrow(InvalidCredentialsException::new);

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new InvalidCredentialsException();
		}

		return issue(user, device, ipAddress);
	}

	private AuthResponse issue(User user, String device, String ipAddress) {
		String refreshToken = this.sessionService.start(user, device, ipAddress);
		return AuthResponse.bearer(this.jwtService.issueToken(user), refreshToken, this.jwtService.getExpirySeconds());
	}

}
