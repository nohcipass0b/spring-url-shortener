package com.aycap.urlshortener.authservice.common.logging;

import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestLoggingFilterTest {

	private final RequestLoggingFilter filter = new RequestLoggingFilter();

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	private ListAppender<ILoggingEvent> appender;

	private ch.qos.logback.classic.Logger logger;

	@BeforeEach
	void setUp() {
		this.request = new MockHttpServletRequest("GET", "/api/urls");
		this.response = new MockHttpServletResponse();
		this.logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
		this.appender = new ListAppender<>();
		this.appender.start();
		this.logger.addAppender(this.appender);
		MDC.clear();
	}

	@AfterEach
	void tearDown() {
		this.logger.detachAppender(this.appender);
		MDC.clear();
	}

	private String onlyMessage() {
		assertThat(this.appender.list).hasSize(1);
		ILoggingEvent event = this.appender.list.get(0);
		assertThat(event.getLevel()).isEqualTo(Level.INFO);
		return event.getFormattedMessage();
	}

	@Test
	void aRequestWithoutAnIdGetsOneAndItComesBackOnTheResponse() throws Exception {
		this.filter.doFilter(this.request, this.response, (req, res) -> {
		});

		String header = this.response.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER);
		assertThat(header).isNotBlank();
	}

	@Test
	void anIdSuppliedUpstreamIsKeptSoTheTracesLineUp() throws Exception {
		this.request.addHeader(RequestLoggingFilter.REQUEST_ID_HEADER, "gateway-abc-123");

		this.filter.doFilter(this.request, this.response, (req, res) -> {
		});

		assertThat(this.response.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER)).isEqualTo("gateway-abc-123");
	}

	@Test
	void aBlankSuppliedIdIsTreatedAsNoIdAtAll() throws Exception {
		this.request.addHeader(RequestLoggingFilter.REQUEST_ID_HEADER, "   ");

		this.filter.doFilter(this.request, this.response, (req, res) -> {
		});

		assertThat(this.response.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER)).isNotBlank().isNotEqualTo("   ");
	}

	@Test
	void anOverlongSuppliedIdIsTruncatedRatherThanTrusted() throws Exception {
		this.request.addHeader(RequestLoggingFilter.REQUEST_ID_HEADER, "x".repeat(500));

		this.filter.doFilter(this.request, this.response, (req, res) -> {
		});

		assertThat(this.response.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER)).hasSize(64);
	}

	@Test
	void theIdIsVisibleToEveryLogLineWrittenDuringTheRequest() throws Exception {
		List<String> seen = new ArrayList<>();
		FilterChain chain = (req, res) -> seen.add(MDC.get(RequestLoggingFilter.REQUEST_ID_KEY));

		this.filter.doFilter(this.request, this.response, chain);

		assertThat(seen).hasSize(1);
		assertThat(seen.get(0)).isNotBlank();
	}

	@Test
	void theIdIsClearedAfterwardsSoItCannotLeakOntoTheNextRequestOnThisThread() throws Exception {
		this.filter.doFilter(this.request, this.response, (req, res) -> {
		});

		assertThat(MDC.get(RequestLoggingFilter.REQUEST_ID_KEY)).isNull();
	}

	@Test
	void oneLineRecordsTheMethodPathStatusAndDuration() throws Exception {
		this.response.setStatus(200);

		this.filter.doFilter(this.request, this.response, (req, res) -> {
		});

		assertThat(onlyMessage()).startsWith("GET /api/urls -> 200 in").endsWith("ms");
	}

	@Test
	void theQueryStringIsKeptSoTheLoggedPathIsTheOneThatWasCalled() throws Exception {
		this.request.setQueryString("page=2&size=10");

		this.filter.doFilter(this.request, this.response, (req, res) -> {
		});

		assertThat(onlyMessage()).contains("/api/urls?page=2&size=10");
	}

	@Test
	void aRequestThatBlowsUpIsStillLoggedAndStillClearsTheId() {
		FilterChain exploding = (req, res) -> {
			throw new IllegalStateException("boom");
		};

		assertThatThrownBy(() -> this.filter.doFilter(this.request, this.response, exploding))
			.isInstanceOf(IllegalStateException.class);

		assertThat(onlyMessage()).startsWith("GET /api/urls ->");
		assertThat(MDC.get(RequestLoggingFilter.REQUEST_ID_KEY)).isNull();
	}

}
