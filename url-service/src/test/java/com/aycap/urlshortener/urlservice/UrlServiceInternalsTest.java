package com.aycap.urlshortener.urlservice;

import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.http.HttpStatus;

import com.aycap.urlshortener.urlservice.common.response.ApiResponse;
import com.aycap.urlshortener.urlservice.common.response.ApiStatus;
import com.aycap.urlshortener.urlservice.common.response.Description;
import com.aycap.urlshortener.urlservice.common.response.StatusCode;
import com.aycap.urlshortener.urlservice.exception.ShortUrlNotFoundException;
import com.aycap.urlshortener.urlservice.model.dto.ShortUrlResponse;
import com.aycap.urlshortener.urlservice.model.dto.ShortenRequest;
import com.aycap.urlshortener.urlservice.model.dto.ShortenResponse;
import com.aycap.urlshortener.urlservice.model.entity.ShortUrl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

class UrlServiceInternalsTest {

	@Test
	void mainHandsTheApplicationClassToSpringBoot() {
		try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
			String[] args = { "--server.port=0" };

			UrlServiceApplication.main(args);

			springApplication.verify(() -> SpringApplication.run(UrlServiceApplication.class, args));
		}
		assertThat(new UrlServiceApplication()).isNotNull();
	}

	@Test
	void theEntityKeepsTheProtectedConstructorHibernateInstantiatesItWith() throws Exception {
		Constructor<ShortUrl> constructor = ShortUrl.class.getDeclaredConstructor();
		constructor.setAccessible(true);

		assertThat(constructor.newInstance()).isNotNull();
	}

	@Test
	void theEnvelopeCarriesDataOnSuccessAndNothingOnError() {
		assertThat(ApiResponse.success("payload").data()).isEqualTo("payload");
		assertThat(ApiResponse.success().data()).isNull();
		assertThat(ApiResponse.error(StatusCode.ERR_NOT_FOUND).data()).isNull();
		assertThat(ApiResponse.error(StatusCode.ERR_VALIDATION, "detail").data()).isEqualTo("detail");
		assertThat(ApiResponse.success("x").status().code()).isEqualTo("SUCCESS");
	}

	@Test
	void apiStatusNamesTheEnumConstantAndCopiesItsDescription() {
		ApiStatus status = ApiStatus.of(StatusCode.ERR_NOT_FOUND);

		assertThat(status.code()).isEqualTo("ERR_NOT_FOUND");
		assertThat(status.description()).isEqualTo(new Description("Resource not found"));
	}

	@Test
	void everyStatusCodeMapsToAnHttpStatus() {
		for (StatusCode code : StatusCode.values()) {
			assertThat(code.en()).isNotBlank();
			assertThat(code.httpStatus()).isNotNull();
		}
		assertThat(StatusCode.valueOf("SUCCESS")).isSameAs(StatusCode.SUCCESS);
		assertThat(StatusCode.SUCCESS.httpStatus()).isEqualTo(HttpStatus.OK);
		assertThat(StatusCode.ERR_VALIDATION.httpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(StatusCode.ERR_NOT_FOUND.httpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(StatusCode.ERR_UNAUTHORIZED.httpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(StatusCode.ERR_INTERNAL.httpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@Test
	void notFoundCarriesTheNotFoundCode() {
		assertThat(new ShortUrlNotFoundException().code()).isEqualTo(StatusCode.ERR_NOT_FOUND);
	}

	@Test
	void theListResponseIsBuiltFromTheEntityPlusItsShortUrl() {
		UUID owner = UUID.randomUUID();
		ShortUrl url = new ShortUrl("abc1234", "https://example.com", owner);

		ShortUrlResponse response = ShortUrlResponse.from(url, "http://localhost:8081/r/abc1234");

		assertThat(response.id()).isEqualTo(url.getId());
		assertThat(response.code()).isEqualTo("abc1234");
		assertThat(response.shortUrl()).isEqualTo("http://localhost:8081/r/abc1234");
		assertThat(response.originalUrl()).isEqualTo("https://example.com");
		assertThat(response.active()).isTrue();
		assertThat(response.clickCount()).isZero();
		assertThat(response.createdAt()).isNotNull();
	}

	@Test
	void theRequestAndCreateResponseRecordsExposeWhatTheyHold() {
		assertThat(new ShortenRequest("https://example.com").originalUrl()).isEqualTo("https://example.com");
		assertThat(new ShortenResponse("http://localhost:8081/r/abc").shortUrl())
			.isEqualTo("http://localhost:8081/r/abc");
	}

	@Test
	void theEntityExposesEverythingTheApiReportsOnIt() {
		UUID owner = UUID.randomUUID();
		Instant before = Instant.now();
		ShortUrl url = new ShortUrl("abc1234", "https://example.com", owner);

		assertThat(url.getCode()).isEqualTo("abc1234");
		assertThat(url.getOriginalUrl()).isEqualTo("https://example.com");
		assertThat(url.getUserId()).isEqualTo(owner);
		assertThat(url.getId()).isNull();
		assertThat(url.getCreatedAt()).isBetween(before.minusSeconds(1), Instant.now().plusSeconds(1));
		assertThat(url.isOwnedBy(owner)).isTrue();
		assertThat(url.isOwnedBy(UUID.randomUUID())).isFalse();

		url.recordClick();
		url.recordClick();
		assertThat(url.getClickCount()).isEqualTo(2);
	}

}
