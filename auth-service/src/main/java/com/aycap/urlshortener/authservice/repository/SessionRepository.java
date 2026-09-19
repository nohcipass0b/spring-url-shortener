package com.aycap.urlshortener.authservice.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aycap.urlshortener.authservice.model.entity.Session;

public interface SessionRepository extends JpaRepository<Session, UUID> {

	Optional<Session> findByTokenHash(String tokenHash);

}
