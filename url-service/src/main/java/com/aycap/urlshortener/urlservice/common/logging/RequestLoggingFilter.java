package com.aycap.urlshortener.urlservice.common.logging;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// One line per request, tagged with an id every other line of that request carries.
// No bodies: the URLs a caller shortens are their own.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

	public static final String REQUEST_ID_HEADER = "X-Request-Id";

	public static final String REQUEST_ID_KEY = "requestId";

	private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {

		String requestId = requestId(request);
		MDC.put(REQUEST_ID_KEY, requestId);
		response.setHeader(REQUEST_ID_HEADER, requestId);

		long start = System.nanoTime();
		try {
			chain.doFilter(request, response);
		}
		finally {
			long ms = (System.nanoTime() - start) / 1_000_000;
			log.info("{} {} -> {} in {}ms", request.getMethod(), path(request), response.getStatus(), ms);
			MDC.remove(REQUEST_ID_KEY);
		}
	}

	// keep the id the gateway sent so its trace and ours line up
	private static String requestId(HttpServletRequest request) {
		String supplied = request.getHeader(REQUEST_ID_HEADER);
		if (supplied == null || supplied.isBlank()) {
			return UUID.randomUUID().toString().substring(0, 8);
		}
		return supplied.length() > 64 ? supplied.substring(0, 64) : supplied;
	}

	private static String path(HttpServletRequest request) {
		String query = request.getQueryString();
		return query == null ? request.getRequestURI() : request.getRequestURI() + "?" + query;
	}

}
