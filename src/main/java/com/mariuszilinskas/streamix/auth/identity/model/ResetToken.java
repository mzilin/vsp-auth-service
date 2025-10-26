package com.mariuszilinskas.streamix.auth.identity.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * This entity describes Reset Tokens used when updating passwords.
 *
 * @author Marius Zilinskas
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "reset_tokens")
public class ResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @JsonIgnore
    @Column(nullable = false)
    private String token;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    public ResetToken(UUID userId) {
        this.userId = userId;
    }

}
