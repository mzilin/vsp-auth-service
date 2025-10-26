package com.mariuszilinskas.streamix.auth.identity.service;

import com.mariuszilinskas.streamix.auth.identity.client.UserFeignClient;
import com.mariuszilinskas.streamix.auth.identity.dto.*;
import com.mariuszilinskas.streamix.auth.identity.exception.*;
import com.mariuszilinskas.streamix.auth.identity.model.Passcode;
import com.mariuszilinskas.streamix.auth.identity.producer.RabbitMQProducer;
import com.mariuszilinskas.streamix.auth.identity.repository.PasscodeRepository;
import com.mariuszilinskas.streamix.auth.identity.util.IdentityUtils;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Service implementation for managing User Passcodes.
 *
 * @author Marius Zilinskas
 */
@Service
@RequiredArgsConstructor
public class PasscodeServiceImpl implements PasscodeService {

    private static final Logger logger = LoggerFactory.getLogger(PasscodeServiceImpl.class);
    private final UserFeignClient userFeignClient;
    private final PasscodeRepository passcodeRepository;
    private final TokenGenerationService tokenGenerationService;
    private final RabbitMQProducer rabbitMQProducer;

    @Override
    @Transactional
    public void verifyPasscode(UUID userId, VerifyPasscodeRequest request) {
        logger.info("Verifying Passcode for User [userId: '{}']", userId);

        Passcode passcode = findPasscodeByUserId(userId);
        if (isPasscodeExpired(passcode))
            throw new PasscodeExpiredException();

        if (!isPasscodeCorrect(passcode, request.passcode()))
            throw new PasscodeValidationException();

        UserResponse user = getUserInfo(userId);

        rabbitMQProducer.sendVerifyAccountMessage(userId);
        var emailRequest = new WelcomeEmailRequest("welcome", user.firstName(), user.email());

        deleteUserPasscodes(userId);
        rabbitMQProducer.sendWelcomeEmailMessage(emailRequest);
    }

    private boolean isPasscodeExpired(Passcode passcode) {
        return passcode.getExpiryDate().isBefore(Instant.now());
    }

    private boolean isPasscodeCorrect(Passcode passcode, String givenPasscode) {
        return passcode.getPasscode().equals(givenPasscode);
    }

    @Override
    @Transactional
    public void createPasscode(UUID userId, String firstName, String email) {
        logger.info("Creating Passcode for User [userId: '{}']", userId);

        String passcode = createNewPasscode(userId);

        var emailRequest = new VerificationEmailRequest("verify", firstName, email, passcode);
        rabbitMQProducer.sendVerificationEmailMessage(emailRequest);
    }

    @Override
    @Transactional
    public void resetPasscode(UUID userId) {
        logger.info("Resetting Passcode for User [userId: '{}']", userId);

        UserResponse response = getUserInfo(userId);
        String passcode = createNewPasscode(userId);

        var emailRequest = new VerificationEmailRequest("verify", response.firstName(), response.email(), passcode);
        rabbitMQProducer.sendVerificationEmailMessage(emailRequest);
    }

    private String createNewPasscode(UUID userId) {
        Passcode passcode = findOrCreatePasscode(userId);
        passcode.setPasscode(tokenGenerationService.generatePasscode());
        passcode.setExpiryDate(Instant.now().plusMillis(IdentityUtils.FIFTEEN_MINUTES_IN_MILLIS));
        passcodeRepository.save(passcode);
        return passcode.getPasscode();
    }

    private Passcode findOrCreatePasscode(UUID userId) {
        return passcodeRepository.findByUserId(userId)
                .orElse(new Passcode(userId));
    }

    private Passcode findPasscodeByUserId(UUID userId) {
        return passcodeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(Passcode.class, "userId", userId));
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
    public void deleteUserPasscodes(UUID userId) {
        logger.info("Deleting Passcodes for User [userId: '{}']", userId);
        passcodeRepository.deleteByUserId(userId);
    }

    @Override
    @Transactional
    public void deleteExpiredPasscodes() {
        logger.info("Deleting Expired Passcodes");
        passcodeRepository.deleteAllByExpiryDateBefore(Instant.now());
    }

}
