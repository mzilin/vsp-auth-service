package com.mariuszilinskas.streamix.auth.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserResponse(

        @NotBlank(message = "firstName cannot be blank")
        String firstName,

        @NotBlank(message = "lastName cannot be blank")
        String lastName,

        @NotBlank(message = "email cannot be blank")
        @Email(message = "email should be valid")
        String email

) {}
