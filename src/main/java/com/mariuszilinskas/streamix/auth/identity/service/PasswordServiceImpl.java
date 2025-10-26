package com.mariuszilinskas.streamix.auth.identity.service;

import com.mariuszilinskas.streamix.auth.identity.client.UserFeignClient;
import com.mariuszilinskas.streamix.auth.identity.dto.*;
import com.mariuszilinskas.streamix.auth.identity.exception.*;
import com.mariuszilinskas.streamix.auth.identity.model.Password;
import com.mariuszilinskas.streamix.auth.identity.model.ResetToken;
import com.mariuszilinskas.streamix.auth.identity.producer.RabbitMQProducer;
import com.mariuszilinskas.streamix.auth.identity.repository.PasswordRepository;
import com.mariuszilinskas.streamix.auth.identity.util.IdentityUtils;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Service implementation for managing User Passwords.
 *
 * @author Marius Zilinskas
 */
@Service
@RequiredArgsConstructor
public class PasswordServiceImpl implements PasswordService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordServiceImpl.class);
    private final UserService userService;
    private final PasswordRepository passwordRepository;
    private final ResetTokenService resetTokenService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RabbitMQProducer rabbitMQProducer;
    private final UserFeignClient userFeignClient;

    @Override
    @Transactional
    public void createNewPassword(CredentialsRequest request) {
        logger.info("Creating Password for User [userId: '{}']", request.userId());
        createEncryptedPassword(request.userId(), request.password());
    }

    @Override
    public void verifyPassword(VerifyPasswordRequest request) {
        logger.info("Verifying Password for User [userId: '{}']", request.userId());
        Password storedPassword = getPasswordByUserId(request.userId());
        validatePassword(request.password(), storedPassword);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        logger.info("Setting Password Reset Token for User [email: '{}']", request.email());

        AuthDetails authDetails = userService.getUserAuthDetailsWithEmail(request.email());
        IdentityUtils.checkUserSuspended(authDetails.status());

        UserResponse response = getUserInfo(authDetails.userId());
        String token = resetTokenService.createResetToken(authDetails.userId());

        var emailRequest = new ResetPasswordEmailRequest("reset", response.firstName(), response.email(), token);
        rabbitMQProducer.sendResetPasswordEmailMessage(emailRequest);
    }

    private UserResponse getUserInfo(UUID userId) {
        try {
            return userFeignClient.getUser(userId);
        } catch (FeignException ex) {
            logger.error("Feign Exception when getting User info: User ID '{}', Status {}, Body {}",
                    userId, ex.status(), ex.contentUTF8());
            throw new UserRetrievalException();
        }
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        ResetToken resetToken = resetTokenService.findResetToken(request.resetToken());
        logger.info("Resetting New Password for User [userId: '{}']", resetToken.getUserId());

        validateResetToken(resetToken, request.resetToken());
        createEncryptedPassword(resetToken.getUserId(), request.password());
    }

    private void validateResetToken(ResetToken resetToken, String givenResetToken) {
        if (isResetTokenExpired(resetToken) || !isResetTokenCorrect(resetToken, givenResetToken)) {
            throw new ResetTokenValidationException();
        }
    }

    private boolean isResetTokenCorrect(ResetToken resetToken, String givenResetToken) {
        return resetToken.getToken().equals(givenResetToken);
    }

    private boolean isResetTokenExpired(ResetToken resetToken) {
        return resetToken.getExpiryDate().isBefore(Instant.now());
    }

    private void createEncryptedPassword(UUID userId, String newPassword) {
        Password password = getOrCreateHashedPassword(userId);
        setHashedPassword(password, newPassword);
    }

    private Password getOrCreateHashedPassword(UUID userId) {
        return passwordRepository.findByUserId(userId)
                .orElse(new Password(userId));
    }

    @Override
    @Transactional
    public void updatePassword(UUID userId, UpdatePasswordRequest request) {
        logger.info("Updating Password for User [userId: '{}']", userId);
        Password password = getPasswordByUserId(userId);
        validatePassword(request.currentPassword(), password);
        setHashedPassword(password, request.newPassword());
    }

    private Password getPasswordByUserId(UUID userId) {
        return passwordRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(Password.class, "userId", userId));
    }

    private void validatePassword(String providedPassword, Password storedPassword) {
        if (!passwordEncoder.matches(providedPassword, storedPassword.getPasswordHash()))
            throw new CredentialsValidationException();
    }

    private void setHashedPassword(Password password, String newPassword) {
        password.setPasswordHash(passwordEncoder.encode(newPassword));
        passwordRepository.save(password);
    }

    @Override
    @Transactional
    public void deleteUserPasswords(UUID userId) {
        logger.info("Deleting Passwords for User [userId: '{}']", userId);
        passwordRepository.deleteByUserId(userId);
    }

}
