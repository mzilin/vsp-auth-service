package com.mariuszilinskas.vsp.authservice.service;

import com.mariuszilinskas.vsp.authservice.dto.AuthDetails;
import com.mariuszilinskas.vsp.authservice.dto.CredentialsRequest;
import com.mariuszilinskas.vsp.authservice.dto.LoginRequest;
import com.mariuszilinskas.vsp.authservice.exception.CredentialsValidationException;
import com.mariuszilinskas.vsp.authservice.exception.JwtTokenValidationException;
import com.mariuszilinskas.vsp.authservice.exception.ResourceNotFoundException;
import com.mariuszilinskas.vsp.authservice.exception.SessionExpiredException;
import com.mariuszilinskas.vsp.authservice.util.AuthUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {

    @Mock
    private PasscodeService passcodeService;

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
    AuthServiceImpl authService;

    private final UUID userId = UUID.randomUUID();
    private final UUID tokenId = UUID.randomUUID();
    private AuthDetails authDetails;

    // ------------------------------------

    @BeforeEach
    void setUp() {
        mockRequest = new MockHttpServletRequest();
        mockResponse = new MockHttpServletResponse();

        authDetails = new AuthDetails(userId, List.of("USER"), List.of());
    }

    // ------------------------------------

    @Test
    void testCreatePasswordAndSetPasscode_NewUserCredentials() {
        // Arrange
        CredentialsRequest request = new CredentialsRequest(userId, "Password1!");

        doNothing().when(passwordService).createNewPassword(request);
        doNothing().when(passcodeService).resetPasscode(userId);

        // Act
        authService.createPasswordAndSetPasscode(request);

        // Assert
        verify(passwordService, times(1)).createNewPassword(request);
        verify(passcodeService, times(1)).resetPasscode(userId);
    }

    // ------------------------------------

    @Test
    void testAuthenticateUser_Success() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest("some@email.com", "Password1!");
        CredentialsRequest credentialsRequest = new CredentialsRequest(userId, loginRequest.password());

        when(userService.getUserAuthDetails(loginRequest.email())).thenReturn(authDetails);
        doNothing().when(passwordService).verifyPassword(credentialsRequest);
        doNothing().when(refreshTokenService).createNewRefreshToken(any(UUID.class), eq(userId));
        doNothing().when(jwtService).setAuthCookies(eq(mockResponse), eq(authDetails), any(UUID.class));

        // Act
        authService.authenticateUser(loginRequest, mockResponse);

        // Assert
        verify(userService, times(1)).getUserAuthDetails(loginRequest.email());
        verify(passwordService, times(1)).verifyPassword(credentialsRequest);
        verify(refreshTokenService, times(1)).createNewRefreshToken(any(UUID.class), eq(userId));
        verify(jwtService, times(1)).setAuthCookies(eq(mockResponse), eq(authDetails), any(UUID.class));
    }

    @Test
    void testAuthenticateUser_InvalidCredentials() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest("test@email.com", "wrongPassword");
        CredentialsRequest credentialsRequest = new CredentialsRequest(userId, loginRequest.password());

        when(userService.getUserAuthDetails(loginRequest.email())).thenReturn(authDetails);
        doThrow(CredentialsValidationException.class).when(passwordService).verifyPassword(credentialsRequest);

        // Act & Assert
        assertThrows(CredentialsValidationException.class, () -> {
            authService.authenticateUser(loginRequest, mockResponse);
        });

        // Assert
        verify(userService, times(1)).getUserAuthDetails(loginRequest.email());
        verify(passwordService, times(1)).verifyPassword(credentialsRequest);

        verify(refreshTokenService, never()).createNewRefreshToken(any(UUID.class), eq(userId));
        verify(jwtService, never()).setAuthCookies(any(HttpServletResponse.class), any(AuthDetails.class), any(UUID.class));
    }

    @Test
    void testAuthenticateUser_NonExistingUser() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest("some@email.com", "Password1!");
        when(userService.getUserAuthDetails(loginRequest.email())).thenThrow(ResourceNotFoundException.class);

        // Act & Assert
        assertThrows(CredentialsValidationException.class, () -> {
            authService.authenticateUser(loginRequest, mockResponse);
        });

        // Assert
        verify(userService, times(1)).getUserAuthDetails(loginRequest.email());

        verify(passwordService, never()).verifyPassword(any(CredentialsRequest.class));
        verify(refreshTokenService, never()).createNewRefreshToken(any(UUID.class), any(UUID.class));
        verify(jwtService, never()).setAuthCookies(any(HttpServletResponse.class), any(AuthDetails.class), any(UUID.class));
    }

    // ------------------------------------

    @Test
    void testRefreshAuthTokens_Success() {
        // Arrange
        String refreshToken = "test_refresh_token";

        when(jwtService.extractRefreshToken(mockRequest)).thenReturn(refreshToken);
        doNothing().when(jwtService).validateRefreshToken(refreshToken);
        when(jwtService.extractAuthDetails(refreshToken, AuthUtils.REFRESH_TOKEN_NAME)).thenReturn(authDetails);
        when(jwtService.extractRefreshTokenId(refreshToken)).thenReturn(tokenId);
        doNothing().when(refreshTokenService).createNewRefreshToken(any(UUID.class), eq(userId));
        doNothing().when(jwtService).setAuthCookies(eq(mockResponse), eq(authDetails), any(UUID.class));
        doNothing().when(refreshTokenService).deleteRefreshToken(tokenId);

        // Act
        authService.refreshTokens(mockRequest, mockResponse);

        // Assert
        verify(jwtService, times(1)).extractRefreshToken(mockRequest);
        verify(jwtService, times(1)).validateRefreshToken(refreshToken);
        verify(jwtService, times(1)).extractAuthDetails(refreshToken, AuthUtils.REFRESH_TOKEN_NAME);
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
        verify(jwtService, never()).extractAuthDetails(anyString(), anyString());
        verify(jwtService, never()).extractRefreshTokenId(anyString());
        verify(refreshTokenService, never()).createNewRefreshToken(any(UUID.class), any(UUID.class));
        verify(jwtService, never()).setAuthCookies(any(HttpServletResponse.class), any(AuthDetails.class), any(UUID.class));
        verify(refreshTokenService, never()).deleteRefreshToken(any(UUID.class));
    }

    @Test
    void testRefreshAuthTokens_RefreshTokenNotFound() {
        // Arrange
        String refreshToken = "test_refresh_token";

        when(jwtService.extractRefreshToken(mockRequest)).thenReturn(refreshToken);
        doThrow(new JwtTokenValidationException()).when(jwtService).validateRefreshToken(refreshToken);

        // Act & Assert
        assertThrows(JwtTokenValidationException.class, () -> {
            authService.refreshTokens(mockRequest, mockResponse);
        });

        // Assert
        verify(jwtService, times(1)).extractRefreshToken(mockRequest);
        verify(jwtService, times(1)).validateRefreshToken(refreshToken);

        verify(jwtService, never()).extractAuthDetails(anyString(), anyString());
        verify(jwtService, never()).extractRefreshTokenId(anyString());
        verify(refreshTokenService, never()).createNewRefreshToken(any(UUID.class), any(UUID.class));
        verify(jwtService, never()).setAuthCookies(any(HttpServletResponse.class), any(AuthDetails.class), any(UUID.class));
        verify(refreshTokenService, never()).deleteRefreshToken(any(UUID.class));
    }

    // ------------------------------------


}
