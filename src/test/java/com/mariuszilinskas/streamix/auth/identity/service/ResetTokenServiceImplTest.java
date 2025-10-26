package com.mariuszilinskas.streamix.auth.identity.service;

import com.mariuszilinskas.streamix.auth.identity.exception.ResourceNotFoundException;
import com.mariuszilinskas.streamix.auth.identity.model.ResetToken;
import com.mariuszilinskas.streamix.auth.identity.repository.ResetTokenRepository;
import com.mariuszilinskas.streamix.auth.identity.util.IdentityUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ResetTokenServiceImplTest {

    @Mock
    private TokenGenerationService tokenGenerationService;

    @Mock
    private ResetTokenRepository resetTokenRepository;

    @InjectMocks
    private ResetTokenServiceImpl resetTokenService;

    private final UUID userId = UUID.randomUUID();
    private final ResetToken resetToken = new ResetToken(userId);
    private final String token = RandomStringUtils.randomAlphanumeric(20);

    // ------------------------------------

    @BeforeEach
    void setUp() {
        resetToken.setToken(token);
        resetToken.setExpiryDate(Instant.now().plusMillis(IdentityUtils.FIFTEEN_MINUTES_IN_MILLIS));
    }

    // ------------------------------------

    @Test
    void testCreateResetToken_NewToken() {
        // Arrange
        ArgumentCaptor<ResetToken> captor = ArgumentCaptor.forClass(ResetToken.class);

        when(resetTokenRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(tokenGenerationService.generateResetToken()).thenReturn(resetToken.getToken());
        when(resetTokenRepository.save(captor.capture())).thenReturn(resetToken);

        // Act
        resetTokenService.createResetToken(userId);

        // Assert
        verify(resetTokenRepository, times(1)).findByUserId(userId);
        verify(resetTokenRepository, times(1)).save(captor.capture());

        ResetToken savedToken = captor.getValue();
        assertEquals(userId, savedToken.getUserId());
        assertEquals(resetToken.getToken(), savedToken.getToken());
        assertEquals(resetToken.getExpiryDate().toEpochMilli(), savedToken.getExpiryDate().toEpochMilli(), 1000);
    }

    @Test
    void testCreateResetToken_ExistingToken() {
        // Arrange
        UUID existingUserId = UUID.randomUUID();
        resetToken.setUserId(existingUserId);
        ArgumentCaptor<ResetToken> captor = ArgumentCaptor.forClass(ResetToken.class);

        when(resetTokenRepository.findByUserId(existingUserId)).thenReturn(Optional.of(resetToken));
        when(tokenGenerationService.generateResetToken()).thenReturn(resetToken.getToken());
        when(resetTokenRepository.save(captor.capture())).thenReturn(resetToken);

        // Act
        resetTokenService.createResetToken(existingUserId);

        // Assert
        verify(resetTokenRepository, times(1)).findByUserId(existingUserId);
        verify(resetTokenRepository, times(1)).save(captor.capture());

        ResetToken savedToken = captor.getValue();
        assertEquals(existingUserId, savedToken.getUserId());
        assertEquals(resetToken.getToken(), savedToken.getToken());
        assertEquals(resetToken.getExpiryDate().toEpochMilli(), savedToken.getExpiryDate().toEpochMilli(), 1000);
    }

    // ------------------------------------

    @Test
    void testFindResetToken_Success() {
        // Arrange
        when(resetTokenRepository.findByToken(resetToken.getToken())).thenReturn(Optional.of(resetToken));

        // Act
        ResetToken foundToken = resetTokenService.findResetToken(resetToken.getToken());

        // Assert
        verify(resetTokenRepository, times(1)).findByToken(resetToken.getToken());

        assertEquals(resetToken.getToken(), foundToken.getToken());
        assertEquals(resetToken.getUserId(), foundToken.getUserId());
        assertEquals(resetToken.getExpiryDate(), foundToken.getExpiryDate());
    }

    @Test
    void testFindResetToken_NotFound() {
        // Arrange
        when(resetTokenRepository.findByToken(resetToken.getToken())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> resetTokenService.findResetToken(resetToken.getToken()));

        // Assert
        verify(resetTokenRepository, times(1)).findByToken(resetToken.getToken());
    }

    // ------------------------------------

    @Test
    void testDeleteResetToken_Success() {
        // Arrange
        doNothing().when(resetTokenRepository).deleteByUserId(userId);

        // Act
        resetTokenService.deleteUserResetTokens(userId);

        // Assert
        verify(resetTokenRepository, times(1)).deleteByUserId(userId);

        when(resetTokenRepository.findByUserId(userId)).thenReturn(Optional.empty());
        assertFalse(resetTokenRepository.findByUserId(userId).isPresent());
    }

    @Test
    void testDeleteResetToken_NonExistingToken() {
        // Arrange
        UUID nonExistentUserId = UUID.randomUUID();
        doNothing().when(resetTokenRepository).deleteByUserId(nonExistentUserId);

        // Act
        resetTokenService.deleteUserResetTokens(nonExistentUserId);

        // Assert
        verify(resetTokenRepository, times(1)).deleteByUserId(nonExistentUserId);
    }

    // ------------------------------------

    @Test
    void testDeleteExpiredResetTokens_Success() {
        // Act
        resetTokenService.deleteExpiredResetTokens();

        // Assert
        verify(resetTokenRepository, times(1)).deleteAllByExpiryDateBefore(any(Instant.class));
    }

}
