package com.aycap.urlshortener.authservice.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.aycap.urlshortener.authservice.model.entity.Session;
import com.aycap.urlshortener.authservice.model.entity.User;
import com.aycap.urlshortener.authservice.repository.SessionRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SessionServiceTest {

	private SessionRepository sessionRepository;

	private SessionService sessionService;

	private final User user = new User("user@example.com", "hash");

	@BeforeEach
	void setUp() {
		this.sessionRepository = mock(SessionRepository.class);
		this.sessionService = new SessionService(this.sessionRepository, 30);
	}

	private Session captureSaved() {
		ArgumentCaptor<Session> captor = ArgumentCaptor.forClass(Session.class);
		verify(this.sessionRepository).save(captor.capture());
		return captor.getValue();
	}

	@Test
	void theRawTokenIsReturnedToTheCallerButOnlyItsHashIsStored() {
		String rawToken = this.sessionService.start(this.user, "curl/8", "127.0.0.1");
		Session saved = captureSaved();

		assertThat(rawToken).isNotBlank();
		assertThat(saved.getTokenHash()).isNotEqualTo(rawToken);
		assertThat(saved.getTokenHash()).isEqualTo(SessionService.hash(rawToken));
	}

	@Test
	void theStoredHashIsSha256HexSoItFitsTheSixtyFourCharacterColumn() {
		this.sessionService.start(this.user, null, null);

		assertThat(captureSaved().getTokenHash()).hasSize(64).matches("[0-9a-f]{64}");
	}

	@Test
	void hashingIsStableForTheSameInputAndDiffersForAnother() {
		assertThat(SessionService.hash("token")).isEqualTo(SessionService.hash("token"));
		assertThat(SessionService.hash("token")).isNotEqualTo(SessionService.hash("other"));
	}

	@Test
	void twoSessionsNeverGetTheSameToken() {
		String first = this.sessionService.start(this.user, null, null);
		String second = this.sessionService.start(this.user, null, null);

		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void theSessionRecordsTheCallerAndExpiresAfterTheConfiguredWindow() {
		this.sessionService.start(this.user, "curl/8", "10.0.0.1");
		Session saved = captureSaved();

		assertThat(saved.getUser()).isSameAs(this.user);
		assertThat(saved.getDevice()).isEqualTo("curl/8");
		assertThat(saved.getIpAddress()).isEqualTo("10.0.0.1");
		assertThat(saved.getExpiresAt()).isAfter(Instant.now().plusSeconds(29 * 24 * 3600));
	}

	// Only reachable by taking SHA-256 away from the JVM, which cannot happen in
	// practice. Kept so the failure path is exercised rather than assumed.
	@Test
	void aJvmWithoutSha256FailsLoudlyRatherThanStoringAWeakerHash() {
		try (org.mockito.MockedStatic<MessageDigest> digest = mockStatic(MessageDigest.class)) {
			digest.when(() -> MessageDigest.getInstance("SHA-256")).thenThrow(new NoSuchAlgorithmException("absent"));

			assertThatThrownBy(() -> SessionService.hash("token")).isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("SHA-256")
				.hasCauseInstanceOf(NoSuchAlgorithmException.class);
		}
	}

}
