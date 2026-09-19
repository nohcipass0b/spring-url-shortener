package com.aycap.urlshortener.urlservice.exception;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.aycap.urlshortener.urlservice.common.response.ApiResponse;
import com.aycap.urlshortener.urlservice.common.response.StatusCode;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

	private final ApiExceptionHandler handler = new ApiExceptionHandler();

	private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/shorten");

	private ListAppender<ILoggingEvent> appender;

	private ch.qos.logback.classic.Logger logger;

	@BeforeEach
	void setUp() {
		this.logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ApiExceptionHandler.class);
		this.appender = new ListAppender<>();
		this.appender.start();
		this.logger.addAppender(this.appender);
	}

	@AfterEach
	void tearDown() {
		this.logger.detachAppender(this.appender);
	}

	private ILoggingEvent onlyEvent() {
		assertThat(this.appender.list).hasSize(1);
		return this.appender.list.get(0);
	}

	@Test
	void anUnexpectedFailureAnswers500WithAnOpaqueCode() {
		ResponseEntity<ApiResponse<Void>> response = this.handler
			.onUnexpected(new IllegalStateException("connection pool exhausted"), this.request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(response.getBody().status().code()).isEqualTo("ERR_INTERNAL");
		assertThat(response.getBody().data()).isNull();
	}

	@Test
	void theInternalMessageIsLoggedWithItsStackTraceAndNeverSentToTheCaller() {
		IllegalStateException cause = new IllegalStateException("connection pool exhausted");

		ResponseEntity<ApiResponse<Void>> response = this.handler.onUnexpected(cause, this.request);

		ILoggingEvent event = onlyEvent();
		assertThat(event.getLevel()).isEqualTo(Level.ERROR);
		assertThat(event.getFormattedMessage()).contains("POST", "/api/shorten");
		assertThat(event.getThrowableProxy().getMessage()).isEqualTo("connection pool exhausted");

		assertThat(response.getBody().toString()).doesNotContain("connection pool exhausted");
	}

	@Test
	void aBrokenRuleIsRecordedWithoutAStackTraceBecauseItIsNotADefect() {
		ResponseEntity<ApiResponse<Void>> response = this.handler.onBusiness(new ShortUrlNotFoundException(),
				this.request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

		ILoggingEvent event = onlyEvent();
		assertThat(event.getLevel()).isEqualTo(Level.WARN);
		assertThat(event.getThrowableProxy()).isNull();
		assertThat(event.getFormattedMessage()).contains("ERR_NOT_FOUND");
	}

	@Test
	void aFrameworkRefusalKeepsTheStatusItChoseAndOnlyGainsTheEnvelope() {
		NoResourceFoundException notRouted = new NoResourceFoundException(HttpMethod.GET, "/api/nope", "/api/nope");

		ResponseEntity<ApiResponse<Void>> response = this.handler.onUnexpected(notRouted, this.request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody().status().code()).isEqualTo("ERR_NOT_FOUND");
		assertThat(onlyEvent().getLevel()).isEqualTo(Level.WARN);
		assertThat(onlyEvent().getThrowableProxy()).isNull();
	}

	@Test
	void aStatusWithNoCodeOfItsOwnStillKeepsItsStatus() {
		HttpRequestMethodNotSupportedException wrongVerb = new HttpRequestMethodNotSupportedException("PATCH");

		ResponseEntity<ApiResponse<Void>> response = this.handler.onUnexpected(wrongVerb, this.request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
		assertThat(response.getBody().status().code()).isEqualTo("ERR_INTERNAL");
	}

	@Test
	void statusesMapBackOntoTheCodeThatCarriesThem() {
		assertThat(StatusCode.forHttpStatus(404)).isEqualTo(StatusCode.ERR_NOT_FOUND);
		assertThat(StatusCode.forHttpStatus(401)).isEqualTo(StatusCode.ERR_UNAUTHORIZED);
		assertThat(StatusCode.forHttpStatus(400)).isEqualTo(StatusCode.ERR_VALIDATION);
		assertThat(StatusCode.forHttpStatus(405)).isEqualTo(StatusCode.ERR_INTERNAL);
		assertThat(StatusCode.forHttpStatus(200)).isEqualTo(StatusCode.ERR_INTERNAL);
	}

}
