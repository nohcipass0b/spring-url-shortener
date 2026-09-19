package com.aycap.urlshortener.urlservice.security;

import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtAuthenticationFilterTest {

	private static final String SECRET = "a-test-secret-that-is-long-enough-for-hmac-sha";

	private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(SECRET);

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	private MockFilterChain chain;

	@BeforeEach
	void setUp() {
		this.request = new MockHttpServletRequest();
		this.response = new MockHttpServletResponse();
		this.chain = new MockFilterChain();
		SecurityContextHolder.clearContext();
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	private static String tokenFor(String subject, String secret, long secondsFromNow) {
		SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		long now = System.currentTimeMillis();
		return Jwts.builder()
			.subject(subject)
			.issuedAt(new Date(now))
			.expiration(new Date(now + secondsFromNow * 1000))
			.signWith(key)
			.compact();
	}

	private static UUID authenticatedId() {
		return (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
	}

	@Test
	void aValidTokenPutsTheUserIdIntoTheSecurityContext() throws Exception {
		UUID id = UUID.randomUUID();
		this.request.addHeader("Authorization", "Bearer " + tokenFor(id.toString(), SECRET, 900));

		this.filter.doFilter(this.request, this.response, this.chain);

		assertThat(authenticatedId()).isEqualTo(id);
		assertThat(CurrentUser.id()).isEqualTo(id);
	}

	@Test
	void aRequestWithoutAnAuthorizationHeaderIsPassedAlongUnauthenticated() throws Exception {
		this.filter.doFilter(this.request, this.response, this.chain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		assertThat(this.chain.getRequest()).isNotNull();
	}

	@Test
	void aHeaderThatIsNotABearerTokenIsIgnored() throws Exception {
		this.request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

		this.filter.doFilter(this.request, this.response, this.chain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	void aTokenSignedWithAnotherSecretLeavesTheContextEmpty() throws Exception {
		this.request.addHeader("Authorization",
				"Bearer " + tokenFor(UUID.randomUUID().toString(), "a-different-secret-also-long-enough-here", 900));

		this.filter.doFilter(this.request, this.response, this.chain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	void anExpiredTokenLeavesTheContextEmpty() throws Exception {
		this.request.addHeader("Authorization", "Bearer " + tokenFor(UUID.randomUUID().toString(), SECRET, -60));

		this.filter.doFilter(this.request, this.response, this.chain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	void aSubjectThatIsNotAUuidLeavesTheContextEmptyRatherThanThrowing() throws Exception {
		this.request.addHeader("Authorization", "Bearer " + tokenFor("not-a-uuid", SECRET, 900));

		this.filter.doFilter(this.request, this.response, this.chain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	void currentUserFailsLoudlyWhenNobodyIsAuthenticated() {
		assertThatThrownBy(CurrentUser::id).isInstanceOf(RuntimeException.class);
	}

	@Test
	void currentUserFailsWhenThePrincipalIsNotAUserId() {
		SecurityContextHolder.getContext()
			.setAuthentication(new UsernamePasswordAuthenticationToken("anonymous", null, java.util.List.of()));

		assertThatThrownBy(CurrentUser::id).isInstanceOf(RuntimeException.class);
	}

}
