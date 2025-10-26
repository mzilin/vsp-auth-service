package com.mariuszilinskas.streamix.auth.identity.service;

import com.mariuszilinskas.streamix.auth.identity.model.RefreshToken;

import java.util.UUID;

public interface RefreshTokenService {

    void createNewRefreshToken(UUID tokenId, UUID userId);

    RefreshToken getRefreshToken(UUID tokenId);

    void deleteRefreshToken(UUID tokenId);

    void deleteUserRefreshTokens(UUID userId);

    void deleteExpiredRefreshTokens();

}
