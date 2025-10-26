package com.mariuszilinskas.streamix.auth.identity.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DataDeletionServiceImplTest {

    @Mock
    private PasscodeService passcodeService;

    @Mock
    private PasswordService passwordService;

    @Mock
    private ResetTokenService resetTokenService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private DataDeletionServiceImpl userAuthDataService;

    private final UUID userId = UUID.randomUUID();

    // ------------------------------------

    @Test
    void testDeleteAllUserAuthData_Success() {
        // Arrange
        doNothing().when(passcodeService).deleteUserPasscodes(userId);
        doNothing().when(passwordService).deleteUserPasswords(userId);
        doNothing().when(resetTokenService).deleteUserResetTokens(userId);
        doNothing().when(refreshTokenService).deleteUserRefreshTokens(userId);

        // Act
        userAuthDataService.deleteUserAuthData(userId);

        // Assert
        verify(passcodeService, times(1)).deleteUserPasscodes(userId);
        verify(passwordService, times(1)).deleteUserPasswords(userId);
        verify(resetTokenService, times(1)).deleteUserResetTokens(userId);
        verify(refreshTokenService, times(1)).deleteUserRefreshTokens(userId);
    }

}
