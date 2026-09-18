package com.aycap.urlshortener.authservice.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aycap.urlshortener.authservice.model.entity.Session;
import com.aycap.urlshortener.authservice.model.entity.User;
import com.aycap.urlshortener.authservice.repository.SessionRepository;

@Service
public class SessionService {

	private static final int TOKEN_BYTES = 32;

	private final SessionRepository sessionRepository;

	private final SecureRandom random = new SecureRandom();

	private final Duration refreshTtl;

	public SessionService(SessionRepository sessionRepository,
			@Value("${app.session.refresh-expiry-days}") long refreshExpiryDays) {
		this.sessionRepository = sessionRepository;
		this.refreshTtl = Duration.ofDays(refreshExpiryDays);
	}

	@Transactional
	public String start(User user, String device, String ipAddress) {
		String rawToken = generateToken();
		sessionRepository
			.save(new Session(user, hash(rawToken), device, ipAddress, Instant.now().plus(this.refreshTtl)));
		return rawToken;
	}

	private String generateToken() {
		byte[] bytes = new byte[TOKEN_BYTES];
		this.random.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	public static String hash(String rawToken) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 is required but unavailable", ex);
		}
	}

}
