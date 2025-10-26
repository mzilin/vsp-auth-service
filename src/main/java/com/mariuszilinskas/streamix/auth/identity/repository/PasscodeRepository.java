package com.mariuszilinskas.streamix.auth.identity.repository;

import com.mariuszilinskas.streamix.auth.identity.model.Passcode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing Passcode entities. Supports standard CRUD operations.
 *
 * @author Marius Zilinskas
 */
@Repository
public interface PasscodeRepository extends JpaRepository<Passcode, UUID> {

    Optional<Passcode> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);

    void deleteAllByExpiryDateBefore(Instant expiryDate);

}
