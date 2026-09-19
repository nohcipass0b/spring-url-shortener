package com.aycap.urlshortener.authservice.common.response;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

	@Test
	void successCarriesTheDataAndTheSuccessCode() {
		ApiResponse<String> response = ApiResponse.success("payload");

		assertThat(response.data()).isEqualTo("payload");
		assertThat(response.status().code()).isEqualTo("SUCCESS");
		assertThat(response.status().description().en()).isEqualTo("Success");
	}

	@Test
	void errorWithoutDataLeavesDataNull() {
		ApiResponse<Void> response = ApiResponse.error(StatusCode.ERR_UNAUTHORIZED);

		assertThat(response.data()).isNull();
		assertThat(response.status().code()).isEqualTo("ERR_UNAUTHORIZED");
	}

	@Test
	void errorCanCarryDetailAlongsideTheCode() {
		ApiResponse<String> response = ApiResponse.error(StatusCode.ERR_VALIDATION, "email is required");

		assertThat(response.data()).isEqualTo("email is required");
		assertThat(response.status().code()).isEqualTo("ERR_VALIDATION");
	}

	@Test
	void apiStatusNamesTheEnumConstantAndCopiesItsDescription() {
		ApiStatus status = ApiStatus.of(StatusCode.ERR_EMAIL_TAKEN);

		assertThat(status.code()).isEqualTo("ERR_EMAIL_TAKEN");
		assertThat(status.description()).isEqualTo(new Description("Email already registered"));
	}

	@Test
	void everyStatusCodeExposesTextAndAnHttpStatus() {
		for (StatusCode code : StatusCode.values()) {
			assertThat(code.en()).isNotBlank();
			assertThat(code.httpStatus()).isNotNull();
		}
		assertThat(StatusCode.valueOf("SUCCESS")).isSameAs(StatusCode.SUCCESS);
	}

	@Test
	void statusCodesMapToTheHttpStatusTheApiDocuments() {
		assertThat(StatusCode.SUCCESS.httpStatus()).isEqualTo(HttpStatus.OK);
		assertThat(StatusCode.ERR_VALIDATION.httpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(StatusCode.ERR_UNAUTHORIZED.httpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(StatusCode.ERR_NOT_FOUND.httpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(StatusCode.ERR_EMAIL_TAKEN.httpStatus()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(StatusCode.ERR_INTERNAL.httpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	}

}
