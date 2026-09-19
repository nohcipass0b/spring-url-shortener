package com.aycap.urlshortener.authservice;

import java.lang.reflect.Constructor;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.aycap.urlshortener.authservice.config.SecurityConfig;
import com.aycap.urlshortener.authservice.model.entity.Session;
import com.aycap.urlshortener.authservice.model.entity.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

/**
 * Covers the pieces that exist for the framework rather than for callers: the entry
 * point, the no-arg constructors JPA needs, and the encoder bean.
 */
class ApplicationAndInternalsTest {

	@Test
	void mainHandsTheApplicationClassToSpringBoot() {
		try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
			String[] args = { "--server.port=0" };

			AuthServiceApplication.main(args);

			springApplication.verify(() -> SpringApplication.run(AuthServiceApplication.class, args));
		}
		assertThat(new AuthServiceApplication()).isNotNull();
	}

	@Test
	void entitiesKeepTheProtectedConstructorHibernateInstantiatesThemWith() throws Exception {
		Constructor<User> user = User.class.getDeclaredConstructor();
		Constructor<Session> session = Session.class.getDeclaredConstructor();
		user.setAccessible(true);
		session.setAccessible(true);

		assertThat(user.newInstance()).isNotNull();
		assertThat(session.newInstance()).isNotNull();
	}

	@Test
	void theEncoderBeanProducesBcryptHashesThatVerify() {
		PasswordEncoder encoder = new SecurityConfig().passwordEncoder();

		String hash = encoder.encode("secret123");

		assertThat(hash).startsWith("$2a$").hasSize(60);
		assertThat(encoder.matches("secret123", hash)).isTrue();
		assertThat(encoder.matches("wrong", hash)).isFalse();
	}

}
