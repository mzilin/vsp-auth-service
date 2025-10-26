package com.mariuszilinskas.streamix.auth.identity.service;

import com.mariuszilinskas.streamix.auth.identity.dto.*;
import com.mariuszilinskas.streamix.auth.identity.enums.UserRole;
import com.mariuszilinskas.streamix.auth.identity.enums.UserStatus;
import com.mariuszilinskas.streamix.auth.identity.exception.*;
import com.mariuszilinskas.streamix.auth.identity.util.IdentityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {

    @Mock
    private PasswordService passwordService;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserService userService;

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private HttpServletResponse mockResponse;

    @InjectMocks
    private AuthServiceImpl authService;

    private final UUID userId = UUID.randomUUID();
    private final UUID tokenId = UUID.randomUUID();
    private final String refreshToken = "test_refresh_token";
    private AuthDetails authDetails;

    // ------------------------------------

    @BeforeEach
    void setUp() {
        mockRequest = new MockHttpServletRequest();
        mockResponse = new MockHttpServletResponse();
        authDetails = new AuthDetails(userId, List.of(UserRole.USER), List.of(), UserStatus.ACTIVE);
    }

    // ------------------------------------

    @Test
    void testAuthenticateUser_Success() {
        // Arrange
        String email = "user@email.com";
        String password = "Password1!";
        LoginRequest loginRequest = new LoginRequest(email, password);
        var passwordRequest = new VerifyPasswordRequest(userId, password);

        when(userService.getUserAuthDetailsWithEmail(email)).thenReturn(authDetails);
        doNothing().when(passwordService).verifyPassword(passwordRequest);
        doNothing().when(refreshTokenService).createNewRefreshToken(any(UUID.class), eq(userId));
        doNothing().when(jwtService).setAuthCookies(eq(mockResponse), eq(authDetails), any(UUID.class));

        // Act
        authService.authenticateUser(loginRequest, mockResponse);

        // Assert
        verify(userService, times(1)).getUserAuthDetailsWithEmail(email);
        verify(passwordService, times(1)).verifyPassword(passwordRequest);
        verify(refreshTokenService, times(1)).createNewRefreshToken(any(UUID.class), eq(userId));
        verify(jwtService, times(1)).setAuthCookies(eq(mockResponse), eq(authDetails), any(UUID.class));
    }

    @Test
    void testAuthenticateUser_InvalidCredentials() {
        // Arrange
        String email = "user@email.com";
        String password = "wrongPassword";
        LoginRequest loginRequest = new LoginRequest(email, password);
        var passwordRequest = new VerifyPasswordRequest(userId, password);

        when(userService.getUserAuthDetailsWithEmail(email)).thenReturn(authDetails);
        doThrow(CredentialsValidationException.class).when(passwordService).verifyPassword(passwordRequest);

        // Act & Assert
        assertThrows(CredentialsValidationException.class, () -> {
            authService.authenticateUser(loginRequest, mockResponse);
        });

        // Assert
        verify(userService, times(1)).getUserAuthDetailsWithEmail(email);
        verify(passwordService, times(1)).verifyPassword(passwordRequest);

        verify(refreshTokenService, never()).createNewRefreshToken(any(UUID.class), eq(userId));
        verify(jwtService, never()).setAuthCookies(any(HttpServletResponse.class), any(AuthDetails.class), any(UUID.class));
    }

    @Test
    void testAuthenticateUser_NonExistingUser() {
        // Arrange
        String email = "user@email.com";
        LoginRequest loginRequest = new LoginRequest(email, "Password1!");
        when(userService.getUserAuthDetailsWithEmail(email)).thenThrow(ResourceNotFoundException.class);

        // Act & Assert
        assertThrows(CredentialsValidationException.class, () -> {
            authService.authenticateUser(loginRequest, mockResponse);
        });

        // Assert
        verify(userService, times(1)).getUserAuthDetailsWithEmail(email);

        verify(passwordService, never()).verifyPassword(any(VerifyPasswordRequest.class));
        verify(refreshTokenService, never()).createNewRefreshToken(any(UUID.class), any(UUID.class));
        verify(jwtService, never()).setAuthCookies(any(HttpServletResponse.class), any(AuthDetails.class), any(UUID.class));
    }

    @Test
    void testAuthenticateUser_UserSuspended() {
        // Arrange
        String email = "user@email.com";
        LoginRequest loginRequest = new LoginRequest(email, "Password1!");
        authDetails = new AuthDetails(userId, List.of(UserRole.USER), List.of(), UserStatus.SUSPENDED);

        when(userService.getUserAuthDetailsWithEmail(email)).thenReturn(authDetails);

        // Act & Assert
        assertThrows(UserStatusAccessException.class, () -> {
            authService.authenticateUser(loginRequest, mockResponse);
        });

        // Assert
        verify(userService, times(1)).getUserAuthDetailsWithEmail(loginRequest.email());
        verify(passwordService, never()).verifyPassword(any(VerifyPasswordRequest.class));
        verify(refreshTokenService, never()).createNewRefreshToken(any(UUID.class), any(UUID.class));
        verify(jwtService, never())
                .setAuthCookies(any(HttpServletResponse.class), any(AuthDetails.class), any(UUID.class));
    }

    // ------------------------------------

    @Test
    void testRefreshAuthTokens_Success() {
        // Arrange
        when(jwtService.extractRefreshToken(mockRequest)).thenReturn(refreshToken);
        doNothing().when(jwtService).validateRefreshToken(refreshToken);
        when(jwtService.extractUserIdFromToken(refreshToken, IdentityUtils.REFRESH_TOKEN_NAME)).thenReturn(userId);
        when(userService.getUserAuthDetailsWithId(userId)).thenReturn(authDetails);
        when(jwtService.extractRefreshTokenId(refreshToken)).thenReturn(tokenId);
        doNothing().when(refreshTokenService).createNewRefreshToken(any(UUID.class), eq(userId));
        doNothing().when(jwtService).setAuthCookies(eq(mockResponse), eq(authDetails), any(UUID.class));
        doNothing().when(refreshTokenService).deleteRefreshToken(tokenId);

        // Act
        authService.refreshTokens(mockRequest, mockResponse);

        // Assert
        verify(jwtService, times(1)).extractRefreshToken(mockRequest);
        verify(jwtService, times(1)).validateRefreshToken(refreshToken);
        verify(jwtService, times(1)).extractUserIdFromToken(refreshToken, IdentityUtils.REFRESH_TOKEN_NAME);
        verify(userService, times(1)).getUserAuthDetailsWithId(userId);
        verify(jwtService, times(1)).extractRefreshTokenId(refreshToken);
        verify(refreshTokenService, times(1)).createNewRefreshToken(any(UUID.class), eq(userId));
        verify(jwtService, times(1)).setAuthCookies(eq(mockResponse), eq(authDetails), any(UUID.class));
        verify(refreshTokenService, times(1)).deleteRefreshToken(tokenId);
    }

    @Test
    void testRefreshAuthTokens_RefreshTokenIsNull() {
        // Arrange
        when(jwtService.extractRefreshToken(mockRequest)).thenReturn(null);

        // Act & Assert
        assertThrows(SessionExpiredException.class, () -> {
            authService.refreshTokens(mockRequest, mockResponse);
        });

        // Assert
        verify(jwtService, times(1)).extractRefreshToken(mockRequest);

        verify(jwtService, never()).validateRefreshToken(anyString());
        verify(jwtService, never()).extractUserIdFromToken(anyString(), anyString());
        verify(userService, never()).getUserAuthDetailsWithId(any(UUID.class));
        verify(jwtService, never()).extractRefreshTokenId(anyString());
        verify(refreshTokenService, never()).createNewRefreshToken(any(UUID.class), any(UUID.class));
        verify(jwtService, never()).setAuthCookies(any(HttpServletResponse.class), any(AuthDetails.class), any(UUID.class));
        verify(refreshTokenService, never()).deleteRefreshToken(any(UUID.class));
    }

    @Test
    void testRefreshAuthTokens_RefreshTokenNotFound() {
        // Arrange
        when(jwtService.extractRefreshToken(mockRequest)).thenReturn(refreshToken);
        doThrow(new JwtTokenValidationException()).when(jwtService).validateRefreshToken(refreshToken);

        // Act & Assert
        assertThrows(JwtTokenValidationException.class, () -> {
            authService.refreshTokens(mockRequest, mockResponse);
        });

        // Assert
        verify(jwtService, times(1)).extractRefreshToken(mockRequest);
        verify(jwtService, times(1)).validateRefreshToken(refreshToken);

        verify(jwtService, never()).extractUserIdFromToken(anyString(), anyString());
        verify(userService, never()).getUserAuthDetailsWithId(any(UUID.class));
        verify(jwtService, never()).extractRefreshTokenId(anyString());
        verify(refreshTokenService, never()).createNewRefreshToken(any(UUID.class), any(UUID.class));
        verify(jwtService, never()).setAuthCookies(any(HttpServletResponse.class), any(AuthDetails.class), any(UUID.class));
        verify(refreshTokenService, never()).deleteRefreshToken(any(UUID.class));
    }

    @Test
    void testRefreshAuthTokens_UserSuspended() {
        // Arrange
        authDetails = new AuthDetails(userId, List.of(UserRole.USER), List.of(), UserStatus.SUSPENDED);

        when(jwtService.extractRefreshToken(mockRequest)).thenReturn(refreshToken);
        doNothing().when(jwtService).validateRefreshToken(refreshToken);
        when(jwtService.extractUserIdFromToken(refreshToken, IdentityUtils.REFRESH_TOKEN_NAME)).thenReturn(userId);
        when(userService.getUserAuthDetailsWithId(userId)).thenReturn(authDetails);

        // Act & Assert
        assertThrows(UserStatusAccessException.class, () -> {
            authService.refreshTokens(mockRequest, mockResponse);
        });

        // Assert
        verify(jwtService, times(1)).extractRefreshToken(mockRequest);
        verify(jwtService, times(1)).validateRefreshToken(refreshToken);
        verify(jwtService, times(1)).extractUserIdFromToken(refreshToken, IdentityUtils.REFRESH_TOKEN_NAME);
        verify(userService, times(1)).getUserAuthDetailsWithId(userId);

        verify(jwtService, never()).extractRefreshTokenId(anyString());
        verify(refreshTokenService, never()).createNewRefreshToken(any(UUID.class), any(UUID.class));
        verify(jwtService, never()).setAuthCookies(any(HttpServletResponse.class), any(AuthDetails.class), any(UUID.class));
        verify(refreshTokenService, never()).deleteRefreshToken(any(UUID.class));
    }

    // ------------------------------------

    @Test
    void testLogoutUser_Success() {
        // Arrange
        UUID tokenId = UUID.randomUUID();

        when(jwtService.extractRefreshToken(mockRequest)).thenReturn(refreshToken);
        when(jwtService.extractRefreshTokenId(refreshToken)).thenReturn(tokenId);
        doNothing().when(refreshTokenService).deleteRefreshToken(tokenId);

        // Act
        authService.logoutUser(mockRequest, mockResponse, userId);

        // Assert
        verify(jwtService, times(1)).extractRefreshToken(mockRequest);
        verify(jwtService, times(1)).extractRefreshTokenId(refreshToken);
        verify(refreshTokenService, times(1)).deleteRefreshToken(tokenId);
        verify(jwtService, times(1)).clearAuthCookies(mockResponse);
    }

    @Test
    void testLogoutUser_RefreshTokenIsNull() {
        // Arrange
        when(jwtService.extractRefreshToken(mockRequest)).thenReturn(null);

        // Act
        authService.logoutUser(mockRequest, mockResponse, userId);

        // Assert
        verify(jwtService, times(1)).extractRefreshToken(mockRequest);
        verify(jwtService, never()).extractRefreshTokenId(anyString());
        verify(refreshTokenService, never()).deleteRefreshToken(any(UUID.class));
        verify(jwtService, times(1)).clearAuthCookies(mockResponse);
    }

    @Test
    void testLogoutUser_RefreshTokenNotFound() {
        // Arrange
        UUID nonExistingId = UUID.randomUUID();

        when(jwtService.extractRefreshToken(mockRequest)).thenReturn(refreshToken);
        when(jwtService.extractRefreshTokenId(refreshToken)).thenReturn(nonExistingId);
        doThrow(new DataIntegrityViolationException("Error deleting refresh token")).when(refreshTokenService).deleteRefreshToken(nonExistingId);

        // Act & Assert
        assertThrows(DataIntegrityViolationException.class, () -> {
            authService.logoutUser(mockRequest, mockResponse, userId);
        });

        // Assert
        verify(jwtService, times(1)).extractRefreshToken(mockRequest);
        verify(jwtService, times(1)).extractRefreshTokenId(anyString());
        verify(refreshTokenService, times(1)).deleteRefreshToken(any(UUID.class));
        verify(jwtService, times(1)).clearAuthCookies(mockResponse);
    }

}
