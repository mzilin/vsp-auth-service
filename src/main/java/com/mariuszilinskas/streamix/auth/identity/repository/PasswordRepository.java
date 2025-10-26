package com.mariuszilinskas.streamix.auth.identity.repository;

import com.mariuszilinskas.streamix.auth.identity.model.Password;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing Password entities. Supports standard CRUD operations.
 *
 * @author Marius Zilinskas
 */
@Repository
public interface PasswordRepository extends JpaRepository<Password, UUID> {

    Optional<Password> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);

}
