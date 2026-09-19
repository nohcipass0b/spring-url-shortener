package com.aycap.urlshortener.urlservice.model.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "short_urls")
public class ShortUrl {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(nullable = false, updatable = false)
	private UUID id;

	@Column(nullable = false, unique = true, updatable = false, length = 16)
	private String code;

	@Column(name = "original_url", nullable = false, updatable = false, length = 2048)
	private String originalUrl;

	/** Owner, taken from the JWT subject. No FK - users live in another service. */
	@Column(name = "user_id", nullable = false, updatable = false)
	private UUID userId;

	@Column(nullable = false)
	private boolean active = true;

	@Column(name = "click_count", nullable = false)
	private long clickCount = 0;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt = Instant.now();

	@Column(name = "deactivated_at")
	private Instant deactivatedAt;

	protected ShortUrl() {
	}

	public ShortUrl(String code, String originalUrl, UUID userId) {
		this.code = code;
		this.originalUrl = originalUrl;
		this.userId = userId;
	}

	public void deactivate() {
		if (active) {
			this.active = false;
			this.deactivatedAt = Instant.now();
		}
	}

	public void recordClick() {
		this.clickCount++;
	}

	public boolean isOwnedBy(UUID candidate) {
		return userId.equals(candidate);
	}

	public UUID getId() {
		return id;
	}

	public String getCode() {
		return code;
	}

	public String getOriginalUrl() {
		return originalUrl;
	}

	public UUID getUserId() {
		return userId;
	}

	public boolean isActive() {
		return active;
	}

	public long getClickCount() {
		return clickCount;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getDeactivatedAt() {
		return deactivatedAt;
	}

}
