package com.mariuszilinskas.vsp.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateCredentialsRequest(
        @NotNull(message = "userId cannot be null")
        UUID userId,

        @NotBlank(message = "email cannot be blank")
        @Email(message = "email must be a valid email address")
        String email,

        @NotBlank(message = "password cannot be blank")
        String password
) {}
