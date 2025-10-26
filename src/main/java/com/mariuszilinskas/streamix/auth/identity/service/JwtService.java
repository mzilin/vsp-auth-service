package com.mariuszilinskas.streamix.auth.identity.service;

import com.mariuszilinskas.streamix.auth.identity.dto.AuthDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.UUID;

public interface JwtService {

    String generateAccessToken(AuthDetails authDetails);

    String generateRefreshToken(UUID tokenId, AuthDetails authDetails);

    void setAuthCookies(HttpServletResponse response, AuthDetails authDetails, UUID tokenId);

    void clearAuthCookies(HttpServletResponse response);

    String extractAccessToken(HttpServletRequest request);

    String extractRefreshToken(HttpServletRequest request);

    void validateAccessToken(String token);

    void validateRefreshToken(String token);

    UUID extractUserIdFromToken(String token, String tokenName);

    UUID extractRefreshTokenId(String token);

}
