package com.mariuszilinskas.streamix.auth.identity.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * This entity describes Refresh Tokens used for authentication.
 *
 * @author Marius Zilinskas
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    public RefreshToken(UUID tokenId, UUID userId) {
        this.id = tokenId;
        this.userId = userId;
    }

}
