package com.mariuszilinskas.streamix.auth.identity.service;

import com.mariuszilinskas.streamix.auth.identity.dto.AuthDetails;
import com.mariuszilinskas.streamix.auth.identity.exception.JwtTokenGenerationException;
import com.mariuszilinskas.streamix.auth.identity.exception.JwtTokenValidationException;
import com.mariuszilinskas.streamix.auth.identity.model.RefreshToken;
import com.mariuszilinskas.streamix.auth.identity.util.IdentityUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service implementation for managing JWT tokens.
 *
 * @author Marius Zilinskas
 */
@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtServiceImpl.class);

    @Value("${app.environment:production}")
    private String environment;

    @Value("${app.accessTokenSecret}")
    private String accessTokenSecret;

    @Value("${app.refreshTokenSecret}")
    private String refreshTokenSecret;

    private final RefreshTokenService refreshTokenService;

    @Override
    public String generateAccessToken(AuthDetails authDetails) {
        try {
            return Jwts.builder()
                    .setSubject(authDetails.userId().toString())
                    .setIssuedAt(new Date())
                    .setExpiration(createExpirationDate(IdentityUtils.ACCESS_TOKEN_EXPIRATION_MILLIS))
                    .claim("roles", convertListToString(authDetails.roles()))
                    .claim("authorities", convertListToString(authDetails.authorities()))
                    .signWith(getAccessTokenSecret())
                    .compact();
        } catch (JwtException ex) {
            throw new JwtTokenGenerationException("Access Token");
        }
    }

    @Override
    public String generateRefreshToken(UUID tokenId, AuthDetails authDetails) {
        try {
            return Jwts.builder()
                    .setSubject(authDetails.userId().toString())
                    .setIssuedAt(new Date())
                    .setExpiration(createExpirationDate(IdentityUtils.REFRESH_TOKEN_EXPIRATION_MILLIS))
                    .claim("tokenId", tokenId.toString())
                    .signWith(getRefreshTokenSecret())
                    .compact();
        } catch (JwtException ex) {
            throw new JwtTokenGenerationException("Refresh Token");
        }
    }

    private static Date createExpirationDate(long expirationTime) {
        return new Date((new Date()).getTime() + expirationTime);
    }

    private <T> List<String> convertListToString(List<T> list) {
        return list.stream()
                .map(Object::toString)
                .collect(Collectors.toList());
    }

    @Override
    public void setAuthCookies(HttpServletResponse response, AuthDetails authDetails, UUID tokenId) {
        response.addHeader("Set-Cookie", createAccessCookie(authDetails).toString());
        response.addHeader("Set-Cookie", createRefreshCookie(tokenId, authDetails).toString());
        logger.info("Auth cookies set for user id: {}", authDetails.userId());
    }

    private ResponseCookie createAccessCookie(AuthDetails authDetails) {
        String accessToken = generateAccessToken(authDetails);
        int accessTokenMaxAge = (int) (IdentityUtils.ACCESS_TOKEN_EXPIRATION_MILLIS / 1000);
        return buildCookie(IdentityUtils.ACCESS_TOKEN_NAME, accessToken, accessTokenMaxAge);
    }

    private ResponseCookie createRefreshCookie( UUID tokenId, AuthDetails authDetails) {
        String refreshToken = generateRefreshToken(tokenId, authDetails);
        int refreshTokenMaxAge = (int) (IdentityUtils.REFRESH_TOKEN_EXPIRATION_MILLIS / 1000);
        return buildCookie(IdentityUtils.REFRESH_TOKEN_NAME, refreshToken, refreshTokenMaxAge);
    }

    private ResponseCookie buildCookie(String name, String value, int maxAge) {
        boolean isSecure = IdentityUtils.PRODUCTION_ENV.equals(environment);
        return ResponseCookie.from(name, value)
                .path("/")
                .maxAge(maxAge)
                .httpOnly(true)
                .secure(isSecure)
                .sameSite(isSecure ? "None" : "Lax")
                .build();
    }

    @Override
    public void clearAuthCookies(HttpServletResponse response) {
        response.addCookie(createExpiringCookie(IdentityUtils.ACCESS_TOKEN_NAME));
        response.addCookie(createExpiringCookie(IdentityUtils.REFRESH_TOKEN_NAME));
        logger.info("Auth cookies were cleared");
    }

    private Cookie createExpiringCookie(String name) {
        boolean isSecure = IdentityUtils.PRODUCTION_ENV.equals(environment);
        Cookie cookie = new Cookie(name, null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        cookie.setSecure(isSecure);
        return cookie;
    }

    @Override
    public String extractAccessToken(HttpServletRequest request) {
        return extractTokenFromRequest(request, IdentityUtils.ACCESS_TOKEN_NAME);
    }

    @Override
    public String extractRefreshToken(HttpServletRequest request) {
        return extractTokenFromRequest(request, IdentityUtils.REFRESH_TOKEN_NAME);
    }

    private String extractTokenFromRequest(HttpServletRequest request, String tokenName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        return Arrays.stream(cookies)
                .filter(cookie -> tokenName.equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);
    }

    @Override
    public void validateAccessToken(String token) {
        checkTokenExpiration(token, IdentityUtils.ACCESS_TOKEN_NAME);
    }

    @Override
    public void validateRefreshToken(String token) {
        checkTokenExpiration(token, IdentityUtils.REFRESH_TOKEN_NAME);
        checkValidRefreshTokenExists(token);
    }

    protected void checkTokenExpiration(String token, String tokenName) {
        Date expiration = extractClaim(token, Claims::getExpiration, tokenName);
        if (expiration.before(new Date()))
            throw new JwtTokenValidationException();
    }

    private void checkValidRefreshTokenExists(String token) {
        UUID tokenId = extractRefreshTokenId(token);
        RefreshToken refreshToken = refreshTokenService.getRefreshToken(tokenId);

        if (refreshToken == null) {
            UUID userId = extractUserIdFromToken(token, IdentityUtils.REFRESH_TOKEN_NAME);
            refreshTokenService.deleteUserRefreshTokens(userId);
            throw new JwtTokenValidationException();
        }

        else if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenService.deleteRefreshToken(tokenId);
            throw new JwtTokenValidationException();
        }
    }

    @Override
    public UUID extractUserIdFromToken(String token, String tokenName) {
        return UUID.fromString(extractClaim(token, Claims::getSubject, tokenName));
    }

    @Override
    public UUID extractRefreshTokenId(String token) {
        String tokenId = extractClaim(token, claims -> claims.get("tokenId", String.class), IdentityUtils.REFRESH_TOKEN_NAME);
        return UUID.fromString(tokenId);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver, String tokenName) {
        Claims claims = extractAllClaims(token, tokenName);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token, String tokenName) {
        return parseToken(token, tokenName).getBody();
    }

    private Jws<Claims> parseToken(String token, String tokenName) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getAuthTokenSecret(tokenName))
                    .build()
                    .parseClaimsJws(token);
        } catch (JwtException ex) {
            System.out.println(ex.getMessage());
            throw new JwtTokenValidationException();
        }
    }

    private SecretKey getAuthTokenSecret(String tokenName) {
        if (IdentityUtils.REFRESH_TOKEN_NAME.equals(tokenName))
            return getRefreshTokenSecret();
        return getAccessTokenSecret();
    }

    private SecretKey getAccessTokenSecret() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(accessTokenSecret));
    }

    private SecretKey getRefreshTokenSecret() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(refreshTokenSecret));
    }

}
