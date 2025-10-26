package com.mariuszilinskas.streamix.auth.identity.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * This entity describes Passcodes used for user account verification.
 *
 * @author Marius Zilinskas
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "passcodes")
public class Passcode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @JoinColumn(name = "user_id", nullable = false)
    private UUID userId;

    @JsonIgnore
    @Column(nullable = false)
    private String passcode;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    public Passcode(UUID userId) {
        this.userId = userId;
    }

}
