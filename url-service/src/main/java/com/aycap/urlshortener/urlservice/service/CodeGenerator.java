package com.aycap.urlshortener.urlservice.service;

import java.security.SecureRandom;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CodeGenerator {

	// base 62 ramdon
	private static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
		.toCharArray();

	private final SecureRandom random = new SecureRandom();

	private final int length;

	public CodeGenerator(@Value("${app.short-url.code-length}") int length) {
		this.length = length;
	}

	public String generate() {
		char[] out = new char[length];
		for (int i = 0; i < length; i++) {
			out[i] = ALPHABET[random.nextInt(ALPHABET.length)];
		}
		return new String(out);
	}

}
