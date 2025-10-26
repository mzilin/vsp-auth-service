package com.mariuszilinskas.streamix.auth.identity.service;

import com.mariuszilinskas.streamix.auth.identity.dto.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.UUID;

public interface AuthService {

    void authenticateUser(LoginRequest request, HttpServletResponse response);

    void refreshTokens(HttpServletRequest request, HttpServletResponse response);

    void logoutUser(HttpServletRequest request, HttpServletResponse response, UUID userId);

}
