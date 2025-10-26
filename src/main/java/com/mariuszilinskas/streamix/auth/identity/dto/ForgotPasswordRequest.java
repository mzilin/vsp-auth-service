package com.mariuszilinskas.streamix.auth.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(

        @NotBlank(message = "email cannot be blank")
        @Email(message = "email should be valid")
        String email

) {
        public ForgotPasswordRequest {
                email = email.trim().toLowerCase();
        }
}
