package com.aycap.urlshortener.urlservice.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.aycap.urlshortener.urlservice.common.response.ApiResponse;
import com.aycap.urlshortener.urlservice.common.response.StatusCode;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	// a rule the caller broke, not a defect, so no stack trace
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> onBusiness(BusinessException ex, HttpServletRequest request) {
		log.warn("{} {} rejected: {} - {}", request.getMethod(), request.getRequestURI(), ex.code(), ex.getMessage());
		return ResponseEntity.status(ex.code().httpStatus()).body(ApiResponse.error(ex.code()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Map<String, String>>> onValidation(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		Map<String, String> fields = new LinkedHashMap<>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			fields.putIfAbsent(error.getField(), error.getDefaultMessage());
		}
		log.warn("{} {} failed validation on {}", request.getMethod(), request.getRequestURI(), fields.keySet());
		return ResponseEntity.status(StatusCode.ERR_VALIDATION.httpStatus())
			.body(ApiResponse.error(StatusCode.ERR_VALIDATION, fields));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> onUnexpected(Exception ex, HttpServletRequest request) {
		// an unrouted path or the wrong verb: the framework already picked the right
		// status, so keep it rather than call a caller's typo a server fault
		if (ex instanceof ErrorResponse framework) {
			int status = framework.getStatusCode().value();
			log.warn("{} {} refused with {}", request.getMethod(), request.getRequestURI(), status);
			return ResponseEntity.status(status).body(ApiResponse.error(StatusCode.forHttpStatus(status)));
		}

		// a defect: stack trace to the log, an opaque code to the caller
		log.error("{} {} failed unexpectedly", request.getMethod(), request.getRequestURI(), ex);
		return ResponseEntity.status(StatusCode.ERR_INTERNAL.httpStatus())
			.body(ApiResponse.error(StatusCode.ERR_INTERNAL));
	}

}
