package com.mariuszilinskas.streamix.auth.identity.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service implementation for managing User Auth Data deletion.
 *
 * @author Marius Zilinskas
 */
@Service
@RequiredArgsConstructor
public class DataDeletionServiceImpl implements DataDeletionService {

    private static final Logger logger = LoggerFactory.getLogger(DataDeletionServiceImpl.class);
    private final PasscodeService passcodeService;
    private final PasswordService passwordService;
    private final ResetTokenService resetTokenService;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional
    public void deleteUserAuthData(UUID userId) {
        logger.info("Deleting all Auth Data for User [userId: '{}']", userId);

        passcodeService.deleteUserPasscodes(userId);
        passwordService.deleteUserPasswords(userId);
        resetTokenService.deleteUserResetTokens(userId);
        refreshTokenService.deleteUserRefreshTokens(userId);
    }

}
