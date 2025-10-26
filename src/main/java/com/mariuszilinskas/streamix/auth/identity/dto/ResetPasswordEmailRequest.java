package com.mariuszilinskas.streamix.auth.identity.dto;

public record ResetPasswordEmailRequest(
        String type,
        String firstName,
        String email,
        String resetToken
) {}
