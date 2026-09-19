package com.aycap.urlshortener.urlservice.controller;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.aycap.urlshortener.urlservice.exception.ShortUrlNotFoundException;
import com.aycap.urlshortener.urlservice.model.entity.ShortUrl;
import com.aycap.urlshortener.urlservice.service.ShortUrlService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = { ShortUrlController.class, RedirectController.class })
class ShortUrlControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ShortUrlService service;

	private final UUID caller = UUID.randomUUID();

	@BeforeEach
	void authenticate() {
		SecurityContextHolder.getContext()
			.setAuthentication(new UsernamePasswordAuthenticationToken(this.caller, null, List.of()));
	}

	@AfterEach
	void clear() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void shortenAnswers201WithTheShortUrl() throws Exception {
		ShortUrl created = new ShortUrl("abc1234", "https://example.com", this.caller);
		when(this.service.shorten("https://example.com", this.caller)).thenReturn(created);
		when(this.service.shortUrlFor(created)).thenReturn("http://localhost:8081/r/abc1234");

		this.mockMvc
			.perform(post("/api/shorten").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"original_url\":\"https://example.com\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status.code").value("SUCCESS"))
			.andExpect(jsonPath("$.data.short_url").value("http://localhost:8081/r/abc1234"));
	}

	@Test
	void shortenRejectsAUrlThatIsNotHttpSoAJavascriptLinkCannotBeStored() throws Exception {
		this.mockMvc
			.perform(post("/api/shorten").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"original_url\":\"javascript:alert(1)\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status.code").value("ERR_VALIDATION"))
			.andExpect(jsonPath("$.data.originalUrl").exists());
	}

	@Test
	void shortenRejectsAMissingUrl() throws Exception {
		this.mockMvc.perform(post("/api/shorten").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status.code").value("ERR_VALIDATION"));
	}

	@Test
	void listAnswersWithTheCallersOwnLinks() throws Exception {
		ShortUrl url = new ShortUrl("abc1234", "https://example.com", this.caller);
		when(this.service.listOwnedBy(this.caller)).thenReturn(List.of(url));
		when(this.service.shortUrlFor(url)).thenReturn("http://localhost:8081/r/abc1234");

		this.mockMvc.perform(get("/api/urls"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[0].code").value("abc1234"))
			.andExpect(jsonPath("$.data[0].short_url").value("http://localhost:8081/r/abc1234"))
			.andExpect(jsonPath("$.data[0].original_url").value("https://example.com"))
			.andExpect(jsonPath("$.data[0].active").value(true))
			.andExpect(jsonPath("$.data[0].click_count").value(0));
	}

	@Test
	void deactivateAnswers200WithTheEnvelopeAndNoData() throws Exception {
		UUID id = UUID.randomUUID();

		this.mockMvc.perform(delete("/api/urls/{id}", id).with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status.code").value("SUCCESS"))
			.andExpect(jsonPath("$.data").doesNotExist());

		verify(this.service).deactivate(id, this.caller);
	}

	@Test
	void activateAnswers200AndAsksTheServiceToBringTheLinkBack() throws Exception {
		UUID id = UUID.randomUUID();

		this.mockMvc.perform(put("/api/urls/{id}/activate", id).with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status.code").value("SUCCESS"));

		verify(this.service).activate(id, this.caller);
	}

	@Test
	void actingOnALinkThatIsNotYoursAnswers404() throws Exception {
		UUID id = UUID.randomUUID();
		doThrow(new ShortUrlNotFoundException()).when(this.service).deactivate(eq(id), any());

		this.mockMvc.perform(delete("/api/urls/{id}", id).with(csrf()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status.code").value("ERR_NOT_FOUND"));
	}

	@Test
	void theRedirectAnswers302AndPointsAtTheOriginalUrl() throws Exception {
		when(this.service.resolve("abc1234")).thenReturn("https://example.com/target");

		this.mockMvc.perform(get("/r/{code}", "abc1234"))
			.andExpect(status().isFound())
			.andExpect(header().string("Location", "https://example.com/target"));
	}

	@Test
	void aRedirectForADeactivatedOrUnknownCodeAnswers404() throws Exception {
		when(this.service.resolve("gone")).thenThrow(new ShortUrlNotFoundException());

		this.mockMvc.perform(get("/r/{code}", "gone"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status.code").value("ERR_NOT_FOUND"));
	}

}
