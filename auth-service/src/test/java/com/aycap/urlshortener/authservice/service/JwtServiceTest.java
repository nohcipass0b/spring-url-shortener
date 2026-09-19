package com.aycap.urlshortener.authservice.service;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.aycap.urlshortener.authservice.model.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

	private static final String SECRET = "a-test-secret-that-is-long-enough-for-hmac-sha";

	private final JwtService jwtService = new JwtService(SECRET, 900);

	private static User userWithId(UUID id) {
		User user = new User("user@example.com", "hash");
		org.springframework.test.util.ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

	@Test
	void anIssuedTokenCarriesTheUserIdAsSubjectAndTheEmailAsAClaim() {
		UUID id = UUID.randomUUID();

		String token = this.jwtService.issueToken(userWithId(id));
		Jws<Claims> parsed = this.jwtService.parse(token);

		assertThat(parsed.getPayload().getSubject()).isEqualTo(id.toString());
		assertThat(parsed.getPayload().get("email")).isEqualTo("user@example.com");
	}

	@Test
	void theExpiryIsTheConfiguredNumberOfSecondsAfterIssue() {
		String token = this.jwtService.issueToken(userWithId(UUID.randomUUID()));
		Claims claims = this.jwtService.parse(token).getPayload();

		long lifetime = claims.getExpiration().toInstant().getEpochSecond()
				- claims.getIssuedAt().toInstant().getEpochSecond();

		assertThat(lifetime).isEqualTo(900);
		assertThat(this.jwtService.getExpirySeconds()).isEqualTo(900);
	}

	@Test
	void aTokenSignedWithAnotherSecretIsRejected() {
		String foreign = new JwtService("a-completely-different-secret-also-long-enough", 900)
			.issueToken(userWithId(UUID.randomUUID()));

		assertThatThrownBy(() -> this.jwtService.parse(foreign)).isInstanceOf(JwtException.class);
	}

	@Test
	void anAlreadyExpiredTokenIsRejected() {
		JwtService expiring = new JwtService(SECRET, -1);
		String token = expiring.issueToken(userWithId(UUID.randomUUID()));

		assertThatThrownBy(() -> this.jwtService.parse(token)).isInstanceOf(JwtException.class);
	}

	@Test
	void garbageIsRejectedRatherThanParsed() {
		assertThatThrownBy(() -> this.jwtService.parse("not-a-jwt")).isInstanceOfAny(JwtException.class,
				IllegalArgumentException.class);
	}

}
