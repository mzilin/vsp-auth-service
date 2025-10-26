package com.mariuszilinskas.streamix.auth.identity.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "email cannot be blank")
        String email,

        @NotBlank(message = "password cannot be blank")
        String password

) {
        public LoginRequest {
                if (email != null) email = email.trim().toLowerCase();
                if (password != null) password = password.trim();
        }
}
