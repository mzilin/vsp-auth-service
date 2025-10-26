package com.mariuszilinskas.streamix.auth.identity.service;

import com.mariuszilinskas.streamix.auth.identity.client.UserFeignClient;
import com.mariuszilinskas.streamix.auth.identity.dto.*;
import com.mariuszilinskas.streamix.auth.identity.enums.UserRole;
import com.mariuszilinskas.streamix.auth.identity.enums.UserStatus;
import com.mariuszilinskas.streamix.auth.identity.exception.*;
import com.mariuszilinskas.streamix.auth.identity.model.Password;
import com.mariuszilinskas.streamix.auth.identity.model.ResetToken;
import com.mariuszilinskas.streamix.auth.identity.producer.RabbitMQProducer;
import com.mariuszilinskas.streamix.auth.identity.repository.PasswordRepository;
import com.mariuszilinskas.streamix.auth.identity.util.IdentityUtils;
import com.mariuszilinskas.streamix.auth.identity.util.TestUtils;
import feign.FeignException;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PasswordServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private ResetTokenService resetTokenService;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private PasswordRepository passwordRepository;

    @Mock
    private UserFeignClient userFeignClient;

    @Mock
    private RabbitMQProducer rabbitMQProducer;

    @InjectMocks
    private PasswordServiceImpl passwordService;

    private final UUID userId = UUID.randomUUID();
    private final String email = "user@email.com";
    private final String token = RandomStringUtils.randomAlphanumeric(20).toLowerCase();
    private final Password password = new Password(userId);
    private final ResetToken resetToken = new ResetToken(userId);
    private final FeignException feignException = TestUtils.createFeignException();

    // ------------------------------------

    @BeforeEach
    void setUp() {
        password.setPasswordHash("encodedPassword");
        resetToken.setToken(token);
        resetToken.setExpiryDate(Instant.now().plusMillis(IdentityUtils.FIFTEEN_MINUTES_IN_MILLIS));
    }

    // ------------------------------------

    @Test
    void testCreateNewPassword_Success() {
        // Arrange
        String newPassword = "Password1";
        ArgumentCaptor<Password> captor = ArgumentCaptor.forClass(Password.class);
        CredentialsRequest request = new CredentialsRequest(userId, "firstName", email, newPassword);

        when(passwordRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(newPassword)).thenReturn(password.getPasswordHash());
        when(passwordRepository.save(captor.capture())).thenReturn(password);

        // Act
        passwordService.createNewPassword(request);

        // Assert
        verify(passwordRepository, times(1)).findByUserId(userId);
        verify(passwordEncoder, times(1)).encode(newPassword);
        verify(passwordRepository, times(1)).save(captor.capture());

        Password savedPassword = captor.getValue();
        assertEquals(password.getPasswordHash(), savedPassword.getPasswordHash());
    }

    // ------------------------------------

    @Test
    void testVerifyPassword_Success() {
        // Arrange
        var request = new VerifyPasswordRequest(userId, "Password1!");

        when(passwordRepository.findByUserId(userId)).thenReturn(Optional.of(password));
        when(passwordEncoder.matches(request.password(), password.getPasswordHash())).thenReturn(true);

        // Act
        passwordService.verifyPassword(request);

        // Assert
        verify(passwordRepository, times(1)).findByUserId(userId);
        verify(passwordEncoder, times(1)).matches(request.password(), password.getPasswordHash());
    }

    @Test
    void testVerifyPassword_IncorrectPassword() {
        // Arrange
        var request = new VerifyPasswordRequest(userId, "IncorrectPassword1!");

        when(passwordRepository.findByUserId(userId)).thenReturn(Optional.of(password));
        when(passwordEncoder.matches(request.password(), password.getPasswordHash())).thenReturn(false);

        // Act & Assert
        assertThrows(CredentialsValidationException.class, () -> passwordService.verifyPassword(request));

        // Assert
        verify(passwordRepository, times(1)).findByUserId(userId);
        verify(passwordEncoder, times(1)).matches(request.password(), password.getPasswordHash());
    }

    @Test
    void testVerifyPassword_PasswordNotFound() {
        // Arrange
        var request = new VerifyPasswordRequest(userId, "Password1!");
        when(passwordRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> passwordService.verifyPassword(request));

        // Assert
        verify(passwordRepository, times(1)).findByUserId(userId);
        verify(passwordEncoder, never()).matches(request.password(), password.getPasswordHash());
    }

    // ------------------------------------

    @Test
    void testForgotPassword_Success() {
        // Arrange
        ForgotPasswordRequest forgotPasswordRequest = new ForgotPasswordRequest(email);
        AuthDetails authDetails = new AuthDetails(userId, List.of(UserRole.USER), List.of(), UserStatus.ACTIVE);
        var userResponse = new UserResponse("firstName", "lastName", email);
        var emailRequest = new ResetPasswordEmailRequest("reset", "firstName", email, resetToken.getToken());

        when(userService.getUserAuthDetailsWithEmail(email)).thenReturn(authDetails);
        when(userFeignClient.getUser(userId)).thenReturn(userResponse);
        when(resetTokenService.createResetToken(userId)).thenReturn(resetToken.getToken());
        doNothing().when(rabbitMQProducer).sendResetPasswordEmailMessage(emailRequest);

        // Act
        passwordService.forgotPassword(forgotPasswordRequest);

        // Assert
        verify(userService, times(1)).getUserAuthDetailsWithEmail(email);
        verify(userFeignClient, times(1)).getUser(userId);
        verify(resetTokenService, times(1)).createResetToken(userId);
        verify(rabbitMQProducer, times(1)).sendResetPasswordEmailMessage(emailRequest);
    }

    @Test
    void testForgotPassword_FailsToFindUser() {
        // Arrange
        ForgotPasswordRequest request = new ForgotPasswordRequest(email);

        doThrow(EmailVerificationException.class).when(userService).getUserAuthDetailsWithEmail(email);

        // Act & Assert
        assertThrows(EmailVerificationException.class, () -> passwordService.forgotPassword(request));

        // Assert
        verify(userService, times(1)).getUserAuthDetailsWithEmail(email);

        verify(userFeignClient, never()).getUser(any(UUID.class));
        verify(resetTokenService, never()).createResetToken(any(UUID.class));
        verify(rabbitMQProducer, never()).sendResetPasswordEmailMessage(any(ResetPasswordEmailRequest.class));
    }

    @Test
    void testForgotPassword_SuspendedUser() {
        // Arrange
        ForgotPasswordRequest request = new ForgotPasswordRequest(email);
        AuthDetails authDetails = new AuthDetails(userId, List.of(UserRole.USER), List.of(), UserStatus.SUSPENDED);

        when(userService.getUserAuthDetailsWithEmail(email)).thenReturn(authDetails);

        // Act & Assert
        assertThrows(UserStatusAccessException.class, () -> passwordService.forgotPassword(request));

        // Assert
        verify(userService, times(1)).getUserAuthDetailsWithEmail(email);

        verify(userFeignClient, never()).getUser(userId);
        verify(resetTokenService, never()).createResetToken(any(UUID.class));
        verify(rabbitMQProducer, never()).sendResetPasswordEmailMessage(any(ResetPasswordEmailRequest.class));
    }

    @Test
    void testForgotPassword_FeignException() {
        // Arrange
        ForgotPasswordRequest request = new ForgotPasswordRequest(email);
        AuthDetails authDetails = new AuthDetails(userId, List.of(UserRole.USER), List.of(), UserStatus.ACTIVE);

        when(userService.getUserAuthDetailsWithEmail(email)).thenReturn(authDetails);
        doThrow(feignException).when(userFeignClient).getUser(userId);

        // Act & Assert
        assertThrows(UserRetrievalException.class, () -> passwordService.forgotPassword(request));

        // Assert
        verify(userService, times(1)).getUserAuthDetailsWithEmail(email);
        verify(userFeignClient, times(1)).getUser(any(UUID.class));

        verify(resetTokenService, never()).createResetToken(any(UUID.class));
        verify(rabbitMQProducer, never()).sendResetPasswordEmailMessage(any(ResetPasswordEmailRequest.class));
    }

    // ------------------------------------

    @Test
    void testResetPassword_Success() {
        // Arrange
        String newPassword = "Password1";
        String newPasswordHash = "HashedPassword";
        password.setPasswordHash(newPasswordHash);

        ArgumentCaptor<Password> captor = ArgumentCaptor.forClass(Password.class);
        ResetPasswordRequest request = new ResetPasswordRequest(newPassword, resetToken.getToken());

        when(resetTokenService.findResetToken(request.resetToken())).thenReturn(resetToken);
        when(passwordEncoder.encode(newPassword)).thenReturn(newPasswordHash);
        when(passwordRepository.save(captor.capture())).thenReturn(password);

        // Act
        passwordService.resetPassword(request);

        // Assert
        verify(resetTokenService, times(1)).findResetToken(resetToken.getToken());
        verify(passwordEncoder, times(1)).encode(newPassword);
        verify(passwordRepository, times(1)).save(captor.capture());

        Password savedPassword = captor.getValue();
        assertEquals(newPasswordHash, savedPassword.getPasswordHash());
    }

    @Test
    void testResetPassword_ExpiredResetToken() {
        // Arrange
        String newPassword = "Password1";
        resetToken.setExpiryDate(Instant.now().minusSeconds(2));
        ResetPasswordRequest request = new ResetPasswordRequest(newPassword, resetToken.getToken());

        when(resetTokenService.findResetToken(request.resetToken())).thenReturn(resetToken);

        // Act & Assert
        assertThrows(ResetTokenValidationException.class, () -> passwordService.resetPassword(request));

        // Assert
        verify(resetTokenService, times(1)).findResetToken(resetToken.getToken());
        verify(passwordEncoder, never()).encode(anyString());
        verify(passwordRepository, never()).save(any(Password.class));
    }

    @Test
    void testResetPassword_IncorrectResetToken() {
        // Arrange
        String newPassword = "Password1";
        String incorrectToken = "incorrect_reset_token";
        ResetPasswordRequest request = new ResetPasswordRequest(newPassword, incorrectToken);

        when(resetTokenService.findResetToken(request.resetToken())).thenReturn(resetToken);

        // Act & Assert
        assertThrows(ResetTokenValidationException.class, () -> passwordService.resetPassword(request));

        // Assert
        verify(resetTokenService, times(1)).findResetToken(incorrectToken);
        verify(passwordEncoder, never()).encode(anyString());
        verify(passwordRepository, never()).save(any(Password.class));
    }

    @Test
    void testResetPassword_ResetTokenNotFound() {
        // Arrange
        String newPassword = "Password1";
        ResetPasswordRequest request = new ResetPasswordRequest(newPassword, resetToken.getToken());

        doThrow(ResourceNotFoundException.class).when(resetTokenService).findResetToken(request.resetToken());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> passwordService.resetPassword(request));

        // Assert
        verify(resetTokenService, times(1)).findResetToken(resetToken.getToken());
        verify(passwordEncoder, never()).encode(anyString());
        verify(passwordRepository, never()).save(any(Password.class));
    }

    // ------------------------------------

    @Test
    void testUpdatePassword_Success() {
        // Arrange
        String currentPassword = "Password1";
        String newPassword = "Password1!";
        String newPasswordHash = "HashedPassword";
        password.setPasswordHash(newPasswordHash);

        ArgumentCaptor<Password> captor = ArgumentCaptor.forClass(Password.class);
        var request = new UpdatePasswordRequest(currentPassword, newPassword);

        when(passwordRepository.findByUserId(userId)).thenReturn(Optional.of(password));
        when(passwordEncoder.matches(request.currentPassword(), password.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn(newPasswordHash);
        when(passwordRepository.save(captor.capture())).thenReturn(password);

        // Act
        passwordService.updatePassword(userId, request);

        // Assert
        verify(passwordRepository, times(1)).findByUserId(userId);
        verify(passwordEncoder, times(1)).matches(request.currentPassword(), password.getPasswordHash());
        verify(passwordEncoder, times(1)).encode(newPassword);
        verify(passwordRepository, times(1)).save(captor.capture());

        Password savedPassword = captor.getValue();
        assertEquals(newPasswordHash, savedPassword.getPasswordHash());
    }

    @Test
    void testUpdatePassword_IncorrectPassword() {
        // Arrange
        String newPasswordHash = "HashedPassword";
        password.setPasswordHash(newPasswordHash);

        var request = new UpdatePasswordRequest("IncorrectPassword1", "Password1!");

        when(passwordRepository.findByUserId(userId)).thenReturn(Optional.of(password));
        when(passwordEncoder.matches(request.currentPassword(), password.getPasswordHash())).thenReturn(false);

        // Act & Assert
        assertThrows(CredentialsValidationException.class, () -> passwordService.updatePassword(userId, request));

        // Assert
        verify(passwordRepository, times(1)).findByUserId(userId);
        verify(passwordEncoder, times(1)).matches(request.currentPassword(), password.getPasswordHash());
        verify(passwordEncoder, never()).encode(anyString());
        verify(passwordRepository, never()).save(any(Password.class));
    }

    @Test
    void testUpdatePassword_PasswordNotFound() {
        // Arrange
        var request = new UpdatePasswordRequest("CurrentPassword1", "NewPassword1!");
        when(passwordRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> passwordService.updatePassword(userId, request));

        // Assert
        verify(passwordRepository, times(1)).findByUserId(userId);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(passwordRepository, never()).save(any(Password.class));
    }

    // ------------------------------------

    @Test
    void testDeleteUserPasswords_Success() {
        // Arrange
        doNothing().when(passwordRepository).deleteByUserId(userId);
        when(passwordRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // Act
        passwordService.deleteUserPasswords(userId);

        // Assert
        verify(passwordRepository, times(1)).deleteByUserId(userId);
        assertFalse(passwordRepository.findByUserId(userId).isPresent());
    }

    @Test
    void testDeleteUserPasswords_NonExistingPassword() {
        // Arrange
        UUID nonExistentUserId = UUID.randomUUID();
        doNothing().when(passwordRepository).deleteByUserId(nonExistentUserId);
        when(passwordRepository.findByUserId(nonExistentUserId)).thenReturn(Optional.empty());

        // Act
        passwordService.deleteUserPasswords(nonExistentUserId);

        // Assert
        verify(passwordRepository, times(1)).deleteByUserId(nonExistentUserId);
        assertFalse(passwordRepository.findByUserId(nonExistentUserId).isPresent());
    }

    // ------------------------------------
}
