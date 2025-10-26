package com.mariuszilinskas.streamix.auth.identity.dto;

public record WelcomeEmailRequest(
        String type,
        String firstName,
        String email
) {}
