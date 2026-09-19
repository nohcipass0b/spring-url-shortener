package com.aycap.urlshortener.urlservice.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aycap.urlshortener.urlservice.model.entity.ShortUrl;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, UUID> {

	Optional<ShortUrl> findByCodeAndActiveTrue(String code);

	boolean existsByCode(String code);

	List<ShortUrl> findByUserIdOrderByCreatedAtDesc(UUID userId);

	Optional<ShortUrl> findByIdAndUserId(UUID id, UUID userId);

}
