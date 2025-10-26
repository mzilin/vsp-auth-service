package com.mariuszilinskas.streamix.auth.identity.service;

import com.mariuszilinskas.streamix.auth.identity.dto.AuthDetails;
import com.mariuszilinskas.streamix.auth.identity.enums.UserRole;
import com.mariuszilinskas.streamix.auth.identity.enums.UserStatus;
import com.mariuszilinskas.streamix.auth.identity.exception.JwtTokenValidationException;
import com.mariuszilinskas.streamix.auth.identity.model.RefreshToken;
import com.mariuszilinskas.streamix.auth.identity.util.TestUtils;
import com.mariuszilinskas.streamix.auth.identity.util.IdentityUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JwtServiceImplTest {

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private HttpServletResponse mockResponse;

    @InjectMocks
    private JwtServiceImpl jwtService;

    private static final String secretKey = TestUtils.secretKey;
    private static final UUID userId = TestUtils.userId;
    private static final UUID tokenId = TestUtils.tokenId;
    private static final String validAccessToken = TestUtils.validAccessToken;
    private static final String validRefreshToken = TestUtils.validRefreshToken;
    private static final String expiredAccessToken = TestUtils.expiredAccessToken;
    private static final String expiredRefreshToken = TestUtils.expiredRefreshToken;
    private static final String invalidToken = TestUtils.invalidToken;

    private AuthDetails authDetails;
    private final RefreshToken refreshToken = new RefreshToken();

    // ------------------------------------

    @BeforeEach
    void setup() throws NoSuchFieldException, IllegalAccessException {
        setPrivateField(jwtService, "accessTokenSecret", secretKey);
        setPrivateField(jwtService, "refreshTokenSecret", secretKey);
        setPrivateField(jwtService, "environment", IdentityUtils.PRODUCTION_ENV);

        authDetails = new AuthDetails(userId, List.of(UserRole.USER), List.of(), UserStatus.ACTIVE);

        refreshToken.setId(tokenId);
        refreshToken.setUserId(userId);
        refreshToken.setExpiryDate(Instant.now().plusSeconds(10));

        mockRequest = mock(HttpServletRequest.class);
        mockResponse = new MockHttpServletResponse();
    }

    private void setPrivateField(Object targetObject, String fieldName, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = targetObject.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(targetObject, value);
    }

    // ------------------------------------

    @Test
    void testGenerateAccessToken() {
        // Act
        String token = jwtService.generateAccessToken(authDetails);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    // ------------------------------------

    @Test
    void testGenerateRefreshToken() {
        // Act
        String token = jwtService.generateRefreshToken(UUID.randomUUID(), authDetails);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    // ------------------------------------

    @Test
    void testSetAuthCookies() {
        // Act
        jwtService.setAuthCookies(mockResponse, authDetails, tokenId);

        // Retrieve all cookies set on the response
        Collection<String> setCookieHeaders = ((MockHttpServletResponse) mockResponse).getHeaders("Set-Cookie");
        boolean accessTokenFound = false, refreshTokenFound = false;

        // Assert
        for (String header : setCookieHeaders) {
            if (header.contains(IdentityUtils.ACCESS_TOKEN_NAME)) {
                accessTokenFound = true;
                assertTrue(header.contains("HttpOnly"));
                assertTrue(header.contains("Path=/"));
                assertTrue(header.contains("Secure"));
                assertTrue(header.contains("SameSite=None"));
            }
            if (header.contains(IdentityUtils.REFRESH_TOKEN_NAME)) {
                refreshTokenFound = true;
                assertTrue(header.contains("HttpOnly"));
                assertTrue(header.contains("Path=/"));
                assertTrue(header.contains("Secure"));
                assertTrue(header.contains("SameSite=None"));
            }
        }

        assertTrue(accessTokenFound);
        assertTrue(refreshTokenFound);
    }

    // ------------------------------------

    @Test
    void testClearAuthCookies() {
        // Act
        jwtService.clearAuthCookies(mockResponse);

        // Retrieve all cookies set on the response
        Collection<String> setCookieHeaders = ((MockHttpServletResponse) mockResponse).getHeaders("Set-Cookie");
        boolean accessTokenFound = false, refreshTokenFound = false;

        // Assert
        for (String header : setCookieHeaders) {
            if (header.contains(IdentityUtils.ACCESS_TOKEN_NAME)) {
                accessTokenFound = true;
                assertTrue(header.contains("HttpOnly"));
                assertTrue(header.contains("Path=/"));
                assertTrue(header.contains("Max-Age=0"));
                assertTrue(header.contains("Secure"));
            }
            if (header.contains(IdentityUtils.REFRESH_TOKEN_NAME)) {
                refreshTokenFound = true;
                assertTrue(header.contains("HttpOnly"));
                assertTrue(header.contains("Path=/"));
                assertTrue(header.contains("Max-Age=0"));
                assertTrue(header.contains("Secure"));
            }
        }

        assertTrue(accessTokenFound);
        assertTrue(refreshTokenFound);
    }

    // ------------------------------------

    @Test
    void testExtractAccessToken_Success() {
        // Arrange
        Cookie[] cookies = {new Cookie(IdentityUtils.ACCESS_TOKEN_NAME, "sampleToken")};
        when(mockRequest.getCookies()).thenReturn(cookies);

        // Act
        String token = jwtService.extractAccessToken(mockRequest);

        // Assert
        assertEquals("sampleToken", token);
    }

    @Test
    void testExtractAccessToken_NoCookies() {
        // Arrange
        when(mockRequest.getCookies()).thenReturn(null);

        // Act
        String token = jwtService.extractAccessToken(mockRequest);

        // Assert
        assertNull(token);
    }

    // ------------------------------------

    @Test
    void testExtractRefreshToken_Success() {
        // Arrange
        Cookie[] cookies = {new Cookie(IdentityUtils.REFRESH_TOKEN_NAME, "sampleToken")};
        when(mockRequest.getCookies()).thenReturn(cookies);

        // Act
        String token = jwtService.extractRefreshToken(mockRequest);

        // Assert
        assertEquals("sampleToken", token);
    }

    @Test
    void testExtractRefreshToken_NoCookies() {
        // Arrange
        when(mockRequest.getCookies()).thenReturn(null);

        // Act
        String token = jwtService.extractRefreshToken(mockRequest);

        // Assert
        assertNull(token);
    }

    // ------------------------------------

    @Test
    void testValidateAccessToken_ValidToken() {
        // Act & Assert
        assertDoesNotThrow(() -> jwtService.validateAccessToken(validAccessToken));
    }

    @Test
    void testValidateAccessToken_InvalidToken() {
        // Act & Assert
        assertThrows(JwtTokenValidationException.class, () -> jwtService.validateAccessToken(invalidToken));
    }

    @Test
    void testValidateAccessToken_ExpiredToken() {
        // Act & Assert
        assertThrows(JwtTokenValidationException.class, () -> jwtService.validateAccessToken(expiredAccessToken));
    }

    // ------------------------------------

    @Test
    void testValidateRefreshToken_ValidToken() {
        // Arrange
        when(refreshTokenService.getRefreshToken(tokenId)).thenReturn(refreshToken);

        // Act & Assert
        assertDoesNotThrow(() -> jwtService.validateRefreshToken(validRefreshToken));

        verify(refreshTokenService, times(1)).getRefreshToken(tokenId);
    }

    @Test
    void testValidateRefreshToken_InvalidToken() {
        // Act & Assert
        assertThrows(JwtTokenValidationException.class, () -> jwtService.validateRefreshToken(invalidToken));
    }

    @Test
    void testValidateRefreshToken_ExpiredToken() {
        // Act & Assert
        assertThrows(JwtTokenValidationException.class, () -> jwtService.validateRefreshToken(expiredRefreshToken));
    }

    @Test
    void testValidateRefreshToken_TokenNotInDatabase() {
        // Arrange
        when(refreshTokenService.getRefreshToken(tokenId)).thenReturn(null);
        doNothing().when(refreshTokenService).deleteUserRefreshTokens(userId);

        // Act & Assert
        assertThrows(JwtTokenValidationException.class, () -> jwtService.validateRefreshToken(validRefreshToken));

        verify(refreshTokenService, times(1)).getRefreshToken(tokenId);
        verify(refreshTokenService, times(1)).deleteUserRefreshTokens(userId);
    }

    @Test
    void testValidateRefreshToken_ExpiredTokenInDatabase() {
        // Arrange
        refreshToken.setExpiryDate(Instant.now().minusMillis(3600));
        when(refreshTokenService.getRefreshToken(tokenId)).thenReturn(refreshToken);
        doNothing().when(refreshTokenService).deleteRefreshToken(tokenId);

        // Act & Assert
        assertThrows(JwtTokenValidationException.class, () -> jwtService.validateRefreshToken(validRefreshToken));

        verify(refreshTokenService, times(1)).getRefreshToken(tokenId);
        verify(refreshTokenService, times(1)).deleteRefreshToken(tokenId);
        verify(refreshTokenService, never()).deleteUserRefreshTokens(userId);
    }

    // ------------------------------------

    @Test
    void testExtractUserIdFromToken_ValidAccessToken() {
        // Act
        UUID response = jwtService.extractUserIdFromToken(validAccessToken, IdentityUtils.ACCESS_TOKEN_NAME);

        // Assert
        assertEquals(userId, response);
    }

    @Test
    void testExtractUserIdFromToken_InvalidAccessToken() {
        // Act & Assert
        assertThrows(JwtTokenValidationException.class, () -> {
            jwtService.extractUserIdFromToken(invalidToken, IdentityUtils.ACCESS_TOKEN_NAME);
        });
    }

    @Test
    void testExtractUserIdFromToken_ValidRefreshToken() {
        // Act
        UUID response = jwtService.extractUserIdFromToken(validRefreshToken, IdentityUtils.REFRESH_TOKEN_NAME);

        // Assert
        assertEquals(userId, response);
    }

    @Test
    void testExtractUserIdFromToken_InvalidRefreshToken() {
        // Act & Assert
        assertThrows(JwtTokenValidationException.class, () -> {
            jwtService.extractUserIdFromToken(invalidToken, IdentityUtils.REFRESH_TOKEN_NAME);
        });
    }

    // ------------------------------------

    @Test
    void testExtractRefreshTokenId_ValidToken() {
        // Act
        UUID refreshTokenId = jwtService.extractRefreshTokenId(validRefreshToken);

        // Assert
        assertEquals(tokenId, refreshTokenId);
    }

    @Test
    void testExtractRefreshTokenId_InvalidToken() {
        // Act & Assert
        assertThrows(JwtTokenValidationException.class, () -> jwtService.extractRefreshTokenId(invalidToken));
    }

}
