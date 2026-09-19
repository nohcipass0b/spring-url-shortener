package com.aycap.urlshortener.urlservice.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aycap.urlshortener.urlservice.exception.ShortUrlNotFoundException;
import com.aycap.urlshortener.urlservice.model.entity.ShortUrl;
import com.aycap.urlshortener.urlservice.repository.ShortUrlRepository;

@Service
public class ShortUrlService {

	private static final int MAX_CODE_ATTEMPTS = 5;

	private final ShortUrlRepository repository;

	private final CodeGenerator codeGenerator;

	private final String baseUrl;

	public ShortUrlService(ShortUrlRepository repository, CodeGenerator codeGenerator,
			@Value("${app.short-url.base-url}") String baseUrl) {
		this.repository = repository;
		this.codeGenerator = codeGenerator;
		// trailing slashes would produce "http://host//r/abc"
		this.baseUrl = baseUrl.replaceAll("/+$", "");
	}

	// A random code can land on one that already exists, so retried
	@Transactional
	public ShortUrl shorten(String originalUrl, UUID userId) {
		for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
			String code = codeGenerator.generate();
			if (!repository.existsByCode(code)) {
				return repository.save(new ShortUrl(code, originalUrl, userId));
			}
		}
		throw new IllegalStateException("Could not generate an unused code after " + MAX_CODE_ATTEMPTS + " attempts");
	}

	// Resolves a code for redirect and counts the click.
	@Transactional
	public String resolve(String code) {
		ShortUrl url = repository.findByCodeAndActiveTrue(code).orElseThrow(ShortUrlNotFoundException::new);
		url.recordClick();
		return url.getOriginalUrl();
	}

	@Transactional(readOnly = true)
	public List<ShortUrl> listOwnedBy(UUID userId) {
		return repository.findByUserIdOrderByCreatedAtDesc(userId);
	}

	@Transactional
	public void deactivate(UUID id, UUID userId) {
		ShortUrl url = repository.findByIdAndUserId(id, userId).orElseThrow(ShortUrlNotFoundException::new);
		url.deactivate();
	}

	@Transactional
	public void activate(UUID id, UUID userId) {
		ShortUrl url = repository.findByIdAndUserId(id, userId).orElseThrow(ShortUrlNotFoundException::new);
		url.activate();
	}

	public String shortUrlFor(ShortUrl url) {
		return baseUrl + "/r/" + url.getCode();
	}

}
