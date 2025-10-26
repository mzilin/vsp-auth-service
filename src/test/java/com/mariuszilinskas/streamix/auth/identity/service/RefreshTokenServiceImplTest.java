package com.mariuszilinskas.streamix.auth.identity.service;

import com.mariuszilinskas.streamix.auth.identity.exception.ResourceNotFoundException;
import com.mariuszilinskas.streamix.auth.identity.model.RefreshToken;
import com.mariuszilinskas.streamix.auth.identity.repository.RefreshTokenRepository;
import com.mariuszilinskas.streamix.auth.identity.util.IdentityUtils;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    private final UUID tokenId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final RefreshToken refreshToken = new RefreshToken();

    @BeforeEach
    void setup() {
        refreshToken.setId(tokenId);
        refreshToken.setUserId(userId);
        refreshToken.setExpiryDate(Instant.now().plusMillis(IdentityUtils.REFRESH_TOKEN_EXPIRATION_MILLIS));
    }

    // ------------------------------------

    @Test
    void testCreateNewRefreshToken_Success() {
        // Arrange
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);

        when(refreshTokenRepository.findByIdAndUserId(tokenId, userId)).thenReturn(Optional.empty());
        when(refreshTokenRepository.save(captor.capture())).thenReturn(refreshToken);

        // Act
        refreshTokenService.createNewRefreshToken(tokenId, userId);

        // Assert
        verify(refreshTokenRepository, times(1)).findByIdAndUserId(tokenId, userId);
        verify(refreshTokenRepository, times(1)).save(captor.capture());

        RefreshToken savedToken = captor.getValue();
        assertEquals(tokenId, savedToken.getId());
        assertEquals(userId, savedToken.getUserId());
        assertEquals(refreshToken.getExpiryDate().toEpochMilli(), savedToken.getExpiryDate().toEpochMilli(), 1000);
    }

    // ------------------------------------

    @Test
    void testGetRefreshToken_Success() {
        // Arrange
        when(refreshTokenRepository.findById(tokenId)).thenReturn(Optional.of(refreshToken));

        // Act
        RefreshToken response = refreshTokenService.getRefreshToken(tokenId);

        // Assert
        assertNotNull(response);
        assertEquals(refreshToken.getId(), response.getId());
        assertEquals(refreshToken.getUserId(), response.getUserId());

        verify(refreshTokenRepository, times(1)).findById(tokenId);
    }

    @Test
    void testGetRefreshToken_NotFound() {
        // Arrange
        when(refreshTokenRepository.findById(tokenId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> refreshTokenService.getRefreshToken(tokenId));

        verify(refreshTokenRepository, times(1)).findById(tokenId);
    }

    // ------------------------------------

    @Test
    void testDeleteRefreshToken_success() {
        // Arrange
        doNothing().when(refreshTokenRepository).deleteById(tokenId);

        // Act
        refreshTokenService.deleteRefreshToken(tokenId);

        // Assert
        verify(refreshTokenRepository, times(1)).deleteById(tokenId);

        when(refreshTokenRepository.findById(tokenId)).thenReturn(Optional.empty());
        assertFalse(refreshTokenRepository.findById(tokenId).isPresent());
    }

    // ------------------------------------

    @Test
    void testDeleteUserRefreshTokens_success() {
        // Arrange
        doNothing().when(refreshTokenRepository).deleteByUserId(userId);

        // Act
        refreshTokenService.deleteUserRefreshTokens(userId);

        // Assert
        verify(refreshTokenRepository, times(1)).deleteByUserId(userId);
    }

    // ------------------------------------

    @Test
    void testDeleteExpiredRefreshTokens_Success() {
        // Act
        refreshTokenService.deleteExpiredRefreshTokens();

        // Assert
        verify(refreshTokenRepository, times(1)).deleteAllByExpiryDateBefore(any(Instant.class));
    }

}
