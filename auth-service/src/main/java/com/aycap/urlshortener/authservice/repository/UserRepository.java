package com.aycap.urlshortener.authservice.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aycap.urlshortener.authservice.model.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {

	boolean existsByEmail(String email);

}
