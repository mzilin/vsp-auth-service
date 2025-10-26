package com.mariuszilinskas.streamix.auth.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdatePasswordRequest (

        @NotBlank(message = "currentPassword cannot be blank")
        String currentPassword,

        @NotBlank(message = "newPassword cannot be blank")
        @Size(min = 8, max = 20, message = "newPassword must be between 8 and 20 characters")
        @Pattern.List({
                @Pattern(regexp = ".*[a-z].*", message = "newPassword must contain at least one lowercase letter"),
                @Pattern(regexp = ".*[A-Z].*", message = "newPassword must contain at least one uppercase letter"),
                @Pattern(regexp = ".*\\d.*", message = "newPassword must contain at least one digit"),
                @Pattern(regexp = ".*[!@#$%^&*(),.?\":{}|<>].*",
                        message = "newPassword must contain at least one special character")
        })
        String newPassword

) {}
