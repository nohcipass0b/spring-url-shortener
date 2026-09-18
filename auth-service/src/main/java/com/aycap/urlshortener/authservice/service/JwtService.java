package com.aycap.urlshortener.authservice.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.aycap.urlshortener.authservice.model.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	private final SecretKey key;

	private final long expirySeconds;

	public JwtService(@Value("${app.jwt.secret}") String secret,
			@Value("${app.jwt.expiry-seconds}") long expirySeconds) {
		// needs at least 256 bits of key material; this throws if the secret is too short
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.expirySeconds = expirySeconds;
	}

	public String issueToken(User user) {
		Instant now = Instant.now();
		return Jwts.builder().subject(String.valueOf(user.getId())).claim("email", user.getEmail())
				.issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(expirySeconds))).signWith(key).compact();
	}

	/** Throws JwtException if the signature is bad or the token has expired. */
	public Jws<Claims> parse(String token) {
		return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
	}

	public long getExpirySeconds() {
		return expirySeconds;
	}

}
