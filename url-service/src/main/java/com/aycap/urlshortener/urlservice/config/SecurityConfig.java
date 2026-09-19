package com.aycap.urlshortener.urlservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.aycap.urlshortener.urlservice.common.response.ApiResponse;
import com.aycap.urlshortener.urlservice.common.response.StatusCode;
import com.aycap.urlshortener.urlservice.security.JwtAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class SecurityConfig {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter)
			throws Exception {
		return http.csrf(csrf -> csrf.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				// MVC forwards 404/405/500 here as a fresh dispatch that carries no
				// authentication, so leaving it closed turns every error into a 401
				.requestMatchers("/error")
				.permitAll()
				.requestMatchers("/actuator/health")
				.permitAll()
				// redirects must stay public
				.requestMatchers("/r/**")
				.permitAll()
				.anyRequest()
				.authenticated())
			.exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedEntryPoint()))
			.httpBasic(basic -> basic.disable())
			.formLogin(form -> form.disable())
			.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
			.build();
	}

	@Bean
	public AuthenticationEntryPoint unauthorizedEntryPoint() {
		return (request, response, authException) -> {
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			MAPPER.writeValue(response.getOutputStream(), ApiResponse.error(StatusCode.ERR_UNAUTHORIZED));
		};
	}

}
