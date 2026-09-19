package com.aycap.urlshortener.authservice.service;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.aycap.urlshortener.authservice.common.response.StatusCode;
import com.aycap.urlshortener.authservice.exception.EmailAlreadyUsedException;
import com.aycap.urlshortener.authservice.exception.InvalidCredentialsException;
import com.aycap.urlshortener.authservice.model.dto.AuthResponse;
import com.aycap.urlshortener.authservice.model.dto.LoginRequest;
import com.aycap.urlshortener.authservice.model.dto.RegisterRequest;
import com.aycap.urlshortener.authservice.model.dto.RegisterResponse;
import com.aycap.urlshortener.authservice.model.entity.User;
import com.aycap.urlshortener.authservice.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

	private UserRepository userRepository;

	private JwtService jwtService;

	private SessionService sessionService;

	private AuthService authService;

	private final PasswordEncoder encoder = new BCryptPasswordEncoder();

	@BeforeEach
	void setUp() {
		this.userRepository = mock(UserRepository.class);
		this.jwtService = mock(JwtService.class);
		this.sessionService = mock(SessionService.class);
		this.authService = new AuthService(this.userRepository, this.encoder, this.jwtService, this.sessionService);
	}

	@Test
	void registerStoresTheUserWithAHashedPasswordRatherThanThePlainOne() {
		when(this.userRepository.existsByEmail("user@example.com")).thenReturn(false);
		when(this.userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		RegisterResponse response = this.authService.register(new RegisterRequest("user@example.com", "secret123"));

		assertThat(response.email()).isEqualTo("user@example.com");

		org.mockito.ArgumentCaptor<User> saved = org.mockito.ArgumentCaptor.forClass(User.class);
		verify(this.userRepository).save(saved.capture());
		assertThat(saved.getValue().getPasswordHash()).isNotEqualTo("secret123");
		assertThat(this.encoder.matches("secret123", saved.getValue().getPasswordHash())).isTrue();
	}

	@Test
	void registerRefusesAnEmailThatAlreadyHasAnAccount() {
		when(this.userRepository.existsByEmail("taken@example.com")).thenReturn(true);

		assertThatThrownBy(() -> this.authService.register(new RegisterRequest("taken@example.com", "secret123")))
			.isInstanceOf(EmailAlreadyUsedException.class)
			.hasMessageContaining("taken@example.com");

		verify(this.userRepository, never()).save(any());
	}

	@Test
	void loginReturnsAnAccessTokenAndTheRefreshTokenTheSessionIssued() {
		User user = new User("user@example.com", this.encoder.encode("secret123"));
		when(this.userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
		when(this.sessionService.start(user, "curl/8", "127.0.0.1")).thenReturn("refresh-token");
		when(this.jwtService.issueToken(user)).thenReturn("access-token");
		when(this.jwtService.getExpirySeconds()).thenReturn(900L);

		AuthResponse response = this.authService.login(new LoginRequest("user@example.com", "secret123"), "curl/8",
				"127.0.0.1");

		assertThat(response.token()).isEqualTo("access-token");
		assertThat(response.refreshToken()).isEqualTo("refresh-token");
		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.expiresInSeconds()).isEqualTo(900);
	}

	@Test
	void loginRejectsAWrongPasswordWithoutStartingASession() {
		User user = new User("user@example.com", this.encoder.encode("secret123"));
		when(this.userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

		assertThatThrownBy(
				() -> this.authService.login(new LoginRequest("user@example.com", "wrong"), "curl/8", "127.0.0.1"))
			.isInstanceOf(InvalidCredentialsException.class);

		verify(this.sessionService, never()).start(any(), anyString(), anyString());
	}

	@Test
	void loginRejectsAnUnknownEmailTheSameWayItRejectsAWrongPassword() {
		when(this.userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

		assertThatThrownBy(
				() -> this.authService.login(new LoginRequest("nobody@example.com", "secret123"), null, null))
			.isInstanceOf(InvalidCredentialsException.class)
			.hasMessage("Invalid email or password");
	}

	@Test
	void bothLoginFailuresCarryTheSameCodeSoTheyCannotBeToldApart() {
		assertThat(new InvalidCredentialsException().code()).isEqualTo(StatusCode.ERR_UNAUTHORIZED);
		assertThat(new EmailAlreadyUsedException("a@b.com").code()).isEqualTo(StatusCode.ERR_EMAIL_TAKEN);
	}

}
