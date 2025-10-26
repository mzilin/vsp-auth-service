package com.mariuszilinskas.streamix.auth.identity.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TokenGenerationServiceTest {

    private final TokenGenerationService tokenGenerationService = new TokenGenerationService();

    // ------------------------------------

    @Test
    void testGeneratePasscode_ReturnsNonNullString() {
        String passcode = tokenGenerationService.generatePasscode();
        assertNotNull(passcode);
    }

    @Test
    void testGeneratePasscode_ReturnsCorrectLength() {
        int expectedLength = 6;
        String passcode = tokenGenerationService.generatePasscode();
        assertEquals(expectedLength, passcode.length());
    }

    @Test
    void testGeneratePasscode_ContainsOnlyAllowedCharacters() {
        String allowedChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        String passcode = tokenGenerationService.generatePasscode();
        assertTrue(passcode.chars().allMatch(c -> allowedChars.contains(String.valueOf((char) c))));
    }

    @Test
    void testGeneratePasscode_DoesntContainNotAllowedCharacters() {
        String notAllowedChars = "abcdefghijklmnopqrstuvwxyz0OI1";
        String passcode = tokenGenerationService.generatePasscode();
        assertFalse(passcode.chars().anyMatch(c -> notAllowedChars.contains(String.valueOf((char) c))));
    }

    @Test
    void testGeneratePasscode_ReturnsUniqueValues() {
        String passcode1 = tokenGenerationService.generatePasscode();
        String passcode2 = tokenGenerationService.generatePasscode();
        assertNotEquals(passcode1, passcode2);
    }

    // ------------------------------------

    @Test
    void testGenerateResetToken_ReturnsNonNullString() {
        String resetToken = tokenGenerationService.generateResetToken();
        assertNotNull(resetToken);
    }

    @Test
    void testGenerateResetToken_ReturnsCorrectLength() {
        int expectedLength = 20;
        String resetToken = tokenGenerationService.generateResetToken();
        assertEquals(expectedLength, resetToken.length());
    }

    @Test
    void testGenerateResetToken_ContainsOnlyAllowedCharacters() {
        String allowedChars = "abcdefghijklmnopqrstuvwxyz0123456789";
        String resetToken = tokenGenerationService.generateResetToken();
        assertTrue(resetToken.chars().allMatch(c -> allowedChars.contains(String.valueOf((char) c))));
    }

    @Test
    void testGenerateResetToken_DoesntContainNotAllowedCharacters() {
        String notAllowedChars = "!@£$%^&*()<>{}";
        String resetToken = tokenGenerationService.generateResetToken();
        assertFalse(resetToken.chars().anyMatch(c -> notAllowedChars.contains(String.valueOf((char) c))));
    }

    @Test
    void testGenerateResetToken_ReturnsUniqueValues() {
        String resetToken1 = tokenGenerationService.generateResetToken();
        String resetToken2 = tokenGenerationService.generateResetToken();
        assertNotEquals(resetToken1, resetToken2);
    }

}
