package com.aycap.urlshortener.authservice.model.entity;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.aycap.urlshortener.authservice.model.dto.AuthResponse;
import com.aycap.urlshortener.authservice.model.dto.LoginRequest;
import com.aycap.urlshortener.authservice.model.dto.RegisterRequest;
import com.aycap.urlshortener.authservice.model.dto.RegisterResponse;

import static org.assertj.core.api.Assertions.assertThat;

class EntityTest {

	@Test
	void aNewUserStampsItsOwnCreationTimeAndHasNoIdUntilItIsSaved() {
		Instant before = Instant.now();
		User user = new User("user@example.com", "hash");

		assertThat(user.getEmail()).isEqualTo("user@example.com");
		assertThat(user.getPasswordHash()).isEqualTo("hash");
		assertThat(user.getCreatedAt()).isBetween(before.minusSeconds(1), Instant.now().plusSeconds(1));
		assertThat(user.getId()).isNull();
	}

	@Test
	void aNewSessionIsActiveAndCarriesTheDeviceItWasStartedFrom() {
		User user = new User("user@example.com", "hash");
		Instant expiry = Instant.now().plusSeconds(3600);
		Session session = new Session(user, "tokenhash", "curl/8", "127.0.0.1", expiry);

		assertThat(session.getUser()).isSameAs(user);
		assertThat(session.getTokenHash()).isEqualTo("tokenhash");
		assertThat(session.getDevice()).isEqualTo("curl/8");
		assertThat(session.getIpAddress()).isEqualTo("127.0.0.1");
		assertThat(session.getExpiresAt()).isEqualTo(expiry);
		assertThat(session.getCreatedAt()).isNotNull();
		assertThat(session.getId()).isNull();
		assertThat(session.getRevokedAt()).isNull();
		assertThat(session.isActive()).isTrue();
	}

	@Test
	void revokingStampsTheTimeAndTakesTheSessionOutOfUse() {
		Session session = new Session(new User("a@b.com", "h"), "hash", null, null, Instant.now().plusSeconds(3600));

		session.revoke();

		assertThat(session.getRevokedAt()).isNotNull();
		assertThat(session.isActive()).isFalse();
	}

	@Test
	void anExpiredSessionIsNotActiveEvenWhileItIsUnrevoked() {
		Session session = new Session(new User("a@b.com", "h"), "hash", null, null, Instant.now().minusSeconds(1));

		assertThat(session.getRevokedAt()).isNull();
		assertThat(session.isActive()).isFalse();
	}

	@Test
	void registerResponseIsBuiltFromASavedUser() {
		User user = new User("user@example.com", "hash");

		RegisterResponse response = RegisterResponse.from(user);

		assertThat(response.id()).isEqualTo(user.getId());
		assertThat(response.email()).isEqualTo("user@example.com");
		assertThat(response.createdAt()).isEqualTo(user.getCreatedAt());
	}

	@Test
	void bearerFillsInTheTokenType() {
		AuthResponse response = AuthResponse.bearer("access", "refresh", 900);

		assertThat(response.token()).isEqualTo("access");
		assertThat(response.refreshToken()).isEqualTo("refresh");
		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.expiresInSeconds()).isEqualTo(900);
	}

	@Test
	void requestRecordsExposeWhatWasSubmitted() {
		RegisterRequest register = new RegisterRequest("user@example.com", "secret123");
		LoginRequest login = new LoginRequest("user@example.com", "secret123");

		assertThat(register.email()).isEqualTo("user@example.com");
		assertThat(register.password()).isEqualTo("secret123");
		assertThat(login.email()).isEqualTo("user@example.com");
		assertThat(login.password()).isEqualTo("secret123");
	}

}
