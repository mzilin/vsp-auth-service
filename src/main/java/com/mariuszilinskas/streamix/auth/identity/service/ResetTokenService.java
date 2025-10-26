package com.mariuszilinskas.streamix.auth.identity.service;

import com.mariuszilinskas.streamix.auth.identity.model.ResetToken;

import java.util.UUID;

public interface ResetTokenService {

    String createResetToken(UUID userId);

    ResetToken findResetToken(String token);

    void deleteUserResetTokens(UUID userid);

    void deleteExpiredResetTokens();

}
