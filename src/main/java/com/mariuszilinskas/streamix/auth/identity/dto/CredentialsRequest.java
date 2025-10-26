package com.mariuszilinskas.streamix.auth.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CredentialsRequest(

        @NotNull(message = "userId cannot be null")
        UUID userId,

        @NotBlank(message = "firstName cannot be blank")
        String firstName,

        @NotBlank(message = "email cannot be blank")
        String email,

        @NotBlank(message = "password cannot be blank")
        String password

) {}
