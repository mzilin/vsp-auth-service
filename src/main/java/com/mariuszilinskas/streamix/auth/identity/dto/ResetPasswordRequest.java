package com.mariuszilinskas.streamix.auth.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(

        @NotBlank(message = "password cannot be blank")
        @Size(min = 8, max = 64, message = "password must be between 8 and 64 characters")
        @Pattern.List({
                @Pattern(regexp = ".*[a-z].*", message = "password must contain at least one lowercase letter"),
                @Pattern(regexp = ".*[A-Z].*", message = "password must contain at least one uppercase letter"),
                @Pattern(regexp = ".*\\d.*", message = "password must contain at least one digit"),
                @Pattern(regexp = ".*[!@#$%^&*(),.?\":{}|<>].*",
                        message = "password must contain at least one special character")
        })
        String password,

        @NotBlank(message = "resetToken cannot be blank")
        String resetToken

) {}
