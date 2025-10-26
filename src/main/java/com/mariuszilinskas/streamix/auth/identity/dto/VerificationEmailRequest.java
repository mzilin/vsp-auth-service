package com.mariuszilinskas.streamix.auth.identity.dto;

public record VerificationEmailRequest(
        String type,
        String firstName,
        String email,
        String passcode
) {}
