package com.mariuszilinskas.streamix.auth.identity.repository;

import com.mariuszilinskas.streamix.auth.identity.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing Refresh Token entities. Supports standard CRUD operations.
 *
 * @author Marius Zilinskas
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByIdAndUserId(UUID id, UUID userId);

    void deleteByUserId(UUID userId);

    void deleteAllByExpiryDateBefore(Instant expiryDate);

}
