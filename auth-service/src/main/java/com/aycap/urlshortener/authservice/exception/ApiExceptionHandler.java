package com.aycap.urlshortener.authservice.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.aycap.urlshortener.authservice.common.response.ApiResponse;
import com.aycap.urlshortener.authservice.common.response.StatusCode;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> onBusiness(BusinessException ex) {
		return ResponseEntity.status(ex.code().httpStatus()).body(ApiResponse.error(ex.code()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Map<String, String>>> onValidation(MethodArgumentNotValidException ex) {
		Map<String, String> fields = new LinkedHashMap<>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			fields.putIfAbsent(error.getField(), error.getDefaultMessage());
		}
		return ResponseEntity.status(StatusCode.ERR_VALIDATION.httpStatus())
			.body(ApiResponse.error(StatusCode.ERR_VALIDATION, fields));
	}

}
