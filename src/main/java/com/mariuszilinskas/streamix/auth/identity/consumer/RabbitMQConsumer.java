package com.mariuszilinskas.streamix.auth.identity.consumer;

import com.mariuszilinskas.streamix.auth.identity.dto.CredentialsRequest;
import com.mariuszilinskas.streamix.auth.identity.service.DataDeletionService;
import com.mariuszilinskas.streamix.auth.identity.service.PasscodeService;
import com.mariuszilinskas.streamix.auth.identity.service.PasswordService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RabbitMQConsumer {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQConsumer.class);
    private final PasscodeService passcodeService;
    private final PasswordService passwordService;
    private final DataDeletionService dataDeletionService;


    @RabbitListener(queues = "${rabbitmq.queues.create-credentials}")
    public void consumeCreateCredentialsMessage(CredentialsRequest request) {
        logger.info("Received request to create credentials for User [userId: {}]", request.userId());
        passwordService.createNewPassword(request);
        passcodeService.createPasscode(request.userId(), request.firstName(), request.email());
    }

    @RabbitListener(queues = "${rabbitmq.queues.reset-passcode}")
    public void consumeResetPasscodeMessage(UUID userId) {
        logger.info("Received request to create passcode for User [userId: {}]", userId);
        passcodeService.resetPasscode(userId);
    }

    @RabbitListener(queues = "${rabbitmq.queues.delete-user-data}")
    public void consumeDeleteUserDataMessage(UUID userId) {
        logger.info("Received request to delete user data for User [userId: {}]", userId);
        dataDeletionService.deleteUserAuthData(userId);
    }

}
