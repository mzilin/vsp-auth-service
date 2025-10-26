package com.mariuszilinskas.streamix.auth.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record VerifyPasswordRequest(

        @NotNull(message = "userId cannot be null")
        UUID userId,

        @NotBlank(message = "password cannot be blank")
        String password

) {}
