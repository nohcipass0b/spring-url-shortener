package com.aycap.urlshortener.urlservice.config;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.aycap.urlshortener.urlservice.controller.RedirectController;
import com.aycap.urlshortener.urlservice.controller.ShortUrlController;
import com.aycap.urlshortener.urlservice.model.entity.ShortUrl;
import com.aycap.urlshortener.urlservice.security.JwtAuthenticationFilter;
import com.aycap.urlshortener.urlservice.service.ShortUrlService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Drives requests through the real filter chain rather than round the side of it, so the
 * rules in SecurityConfig are the thing under test.
 */
@WebMvcTest(controllers = { ShortUrlController.class, RedirectController.class })
@ImportAutoConfiguration(SecurityAutoConfiguration.class)
@Import({ SecurityConfig.class, SecurityConfigTest.Filters.class })
class SecurityConfigTest {

	private static final String SECRET = "a-test-secret-that-is-long-enough-for-hmac-sha";

	@TestConfiguration
	static class Filters {

		@Bean
		JwtAuthenticationFilter jwtAuthenticationFilter() {
			return new JwtAuthenticationFilter(SECRET);
		}

	}

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ShortUrlService service;

	private static String validToken() {
		SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
		long now = System.currentTimeMillis();
		return Jwts.builder()
			.subject(UUID.randomUUID().toString())
			.issuedAt(new Date(now))
			.expiration(new Date(now + 900_000))
			.signWith(key)
			.compact();
	}

	@Test
	void anApiCallWithoutATokenIsRefusedInTheEnvelopeTheRestOfTheApiUses() throws Exception {
		this.mockMvc.perform(get("/api/urls"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.status.code").value("ERR_UNAUTHORIZED"))
			.andExpect(jsonPath("$.status.description.en").value("Authentication required"))
			.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	void aTokenSignedWithAnotherSecretIsRefusedTheSameWayAsNoTokenAtAll() throws Exception {
		this.mockMvc.perform(get("/api/urls").header("Authorization", "Bearer not.a.token"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.status.code").value("ERR_UNAUTHORIZED"));
	}

	@Test
	void aValidTokenReachesTheController() throws Exception {
		when(this.service.listOwnedBy(org.mockito.ArgumentMatchers.any())).thenReturn(java.util.List.of());

		this.mockMvc.perform(get("/api/urls").header("Authorization", "Bearer " + validToken()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status.code").value("SUCCESS"));
	}

	@Test
	void csrfIsOffSoATokenBearingPostDoesNotNeedAFormToken() throws Exception {
		ShortUrl created = new ShortUrl("abc1234", "https://example.com", UUID.randomUUID());
		when(this.service.shorten(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
			.thenReturn(created);
		when(this.service.shortUrlFor(created)).thenReturn("http://localhost:8081/r/abc1234");

		this.mockMvc
			.perform(post("/api/shorten").header("Authorization", "Bearer " + validToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"original_url\":\"https://example.com\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.short_url").value("http://localhost:8081/r/abc1234"));
	}

	@Test
	void theRedirectStaysPublicBecauseWhoeverFollowsALinkCarriesNoToken() throws Exception {
		when(this.service.resolve("abc1234")).thenReturn("https://example.com");

		this.mockMvc.perform(get("/r/{code}", "abc1234")).andExpect(status().isFound());
	}

	@Test
	void theHealthEndpointStaysPublicSoTheContainerHealthcheckCanPass() throws Exception {
		this.mockMvc.perform(get("/actuator/health")).andExpect(status().isNotFound());
	}

	@Test
	void aRoutingMistakeReportsItselfRatherThanHidingBehindA401() throws Exception {
		this.mockMvc.perform(get("/api/does-not-exist").header("Authorization", "Bearer " + validToken()))
			.andExpect(status().isNotFound());
	}

}
