package com.mariuszilinskas.streamix.auth.identity.repository;

import com.mariuszilinskas.streamix.auth.identity.model.ResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing Reset Token entities. Supports standard CRUD operations.
 *
 * @author Marius Zilinskas
 */
@Repository
public interface ResetTokenRepository extends JpaRepository<ResetToken, UUID> {

    Optional<ResetToken> findByToken(String token);

    Optional<ResetToken> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);

    void deleteAllByExpiryDateBefore(Instant expiryDate);

}
