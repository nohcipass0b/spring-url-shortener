package com.aycap.urlshortener.authservice.controller;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.aycap.urlshortener.authservice.config.SecurityConfig;
import com.aycap.urlshortener.authservice.exception.EmailAlreadyUsedException;
import com.aycap.urlshortener.authservice.exception.InvalidCredentialsException;
import com.aycap.urlshortener.authservice.model.dto.AuthResponse;
import com.aycap.urlshortener.authservice.model.dto.RegisterResponse;
import com.aycap.urlshortener.authservice.service.AuthService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@ImportAutoConfiguration(SecurityAutoConfiguration.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthService authService;

	@Test
	void registerAnswers201WithTheCreatedUser() throws Exception {
		UUID id = UUID.randomUUID();
		when(this.authService.register(any()))
			.thenReturn(new RegisterResponse(id, "user@example.com", Instant.parse("2026-01-01T00:00:00Z")));

		this.mockMvc
			.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"user@example.com\",\"password\":\"secret123\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status.code").value("SUCCESS"))
			.andExpect(jsonPath("$.data.id").value(id.toString()))
			.andExpect(jsonPath("$.data.email").value("user@example.com"));
	}

	@Test
	void registerAnswers409WhenTheEmailIsAlreadyRegistered() throws Exception {
		when(this.authService.register(any())).thenThrow(new EmailAlreadyUsedException("user@example.com"));

		this.mockMvc
			.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"user@example.com\",\"password\":\"secret123\"}"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.status.code").value("ERR_EMAIL_TAKEN"))
			.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	void registerAnswers400AndNamesEveryFieldThatFailedValidation() throws Exception {
		this.mockMvc
			.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"not-an-email\",\"password\":\"short\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status.code").value("ERR_VALIDATION"))
			.andExpect(jsonPath("$.data.email").exists())
			.andExpect(jsonPath("$.data.password").exists());
	}

	@Test
	void loginAnswers200WithATokenPair() throws Exception {
		when(this.authService.login(any(), nullable(String.class), nullable(String.class)))
			.thenReturn(AuthResponse.bearer("access", "refresh", 900));

		this.mockMvc
			.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
				.header("User-Agent", "curl/8")
				.content("{\"email\":\"user@example.com\",\"password\":\"secret123\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.token").value("access"))
			.andExpect(jsonPath("$.data.refreshToken").value("refresh"))
			.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.data.expiresInSeconds").value(900));
	}

	@Test
	void loginAnswers401OnBadCredentials() throws Exception {
		when(this.authService.login(any(), nullable(String.class), nullable(String.class)))
			.thenThrow(new InvalidCredentialsException());

		this.mockMvc
			.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"user@example.com\",\"password\":\"wrong\"}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.status.code").value("ERR_UNAUTHORIZED"));
	}

	@Test
	void aVeryLongUserAgentIsTruncatedBeforeItReachesTheService() throws Exception {
		when(this.authService.login(any(), nullable(String.class), nullable(String.class)))
			.thenReturn(AuthResponse.bearer("access", "refresh", 900));

		this.mockMvc
			.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
				.header("User-Agent", "u".repeat(400))
				.header("X-Forwarded-For", "203.0.113.7, 10.0.0.1")
				.content("{\"email\":\"user@example.com\",\"password\":\"secret123\"}"))
			.andExpect(status().isOk());

		org.mockito.ArgumentCaptor<String> device = org.mockito.ArgumentCaptor.forClass(String.class);
		org.mockito.ArgumentCaptor<String> ip = org.mockito.ArgumentCaptor.forClass(String.class);
		org.mockito.Mockito.verify(this.authService).login(any(), device.capture(), ip.capture());

		org.assertj.core.api.Assertions.assertThat(device.getValue()).hasSize(255);
		org.assertj.core.api.Assertions.assertThat(ip.getValue()).isEqualTo("203.0.113.7");
	}

	@Test
	void aBlankUserAgentIsRecordedAsNoDeviceAtAll() throws Exception {
		when(this.authService.login(any(), nullable(String.class), nullable(String.class)))
			.thenReturn(AuthResponse.bearer("access", "refresh", 900));

		this.mockMvc
			.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
				.header("User-Agent", "   ")
				.content("{\"email\":\"user@example.com\",\"password\":\"secret123\"}"))
			.andExpect(status().isOk());

		org.mockito.Mockito.verify(this.authService)
			.login(any(), org.mockito.ArgumentMatchers.isNull(), nullable(String.class));
	}

	@Test
	void aBlankForwardedForHeaderFallsBackToTheSocketAddress() throws Exception {
		when(this.authService.login(any(), nullable(String.class), nullable(String.class)))
			.thenReturn(AuthResponse.bearer("access", "refresh", 900));

		this.mockMvc
			.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
				.header("X-Forwarded-For", "   ")
				.content("{\"email\":\"user@example.com\",\"password\":\"secret123\"}"))
			.andExpect(status().isOk());

		org.mockito.ArgumentCaptor<String> ip = org.mockito.ArgumentCaptor.forClass(String.class);
		org.mockito.Mockito.verify(this.authService).login(any(), nullable(String.class), ip.capture());

		org.assertj.core.api.Assertions.assertThat(ip.getValue()).isEqualTo("127.0.0.1");
	}

}
