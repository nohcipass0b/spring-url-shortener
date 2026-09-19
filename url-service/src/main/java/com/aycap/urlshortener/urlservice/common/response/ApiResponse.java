package com.aycap.urlshortener.urlservice.common.response;

public record ApiResponse<T>(ApiStatus status, T data) {

	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(ApiStatus.of(StatusCode.SUCCESS), data);
	}

	public static <T> ApiResponse<T> success() {
		return new ApiResponse<>(ApiStatus.of(StatusCode.SUCCESS), null);
	}

	public static <T> ApiResponse<T> error(StatusCode code) {
		return new ApiResponse<>(ApiStatus.of(code), null);
	}

	public static <T> ApiResponse<T> error(StatusCode code, T data) {
		return new ApiResponse<>(ApiStatus.of(code), data);
	}
}
