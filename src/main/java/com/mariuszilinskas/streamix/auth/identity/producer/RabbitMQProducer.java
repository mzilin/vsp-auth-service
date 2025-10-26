package com.mariuszilinskas.streamix.auth.identity.producer;

import com.mariuszilinskas.streamix.auth.identity.dto.ResetPasswordEmailRequest;
import com.mariuszilinskas.streamix.auth.identity.dto.VerificationEmailRequest;
import com.mariuszilinskas.streamix.auth.identity.dto.WelcomeEmailRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RabbitMQProducer {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQProducer.class);
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-keys.verify-account}")
    private String verifyAccountRoutingKey;

    @Value("${rabbitmq.routing-keys.platform-emails}")
    private String platformEmailsRoutingKey;

    public void sendVerifyAccountMessage(UUID userId) {
        logger.info("Sending Verify Account message: [userId: {}]", userId);
        rabbitTemplate.convertAndSend(exchange, verifyAccountRoutingKey, userId);
    }

    public void sendVerificationEmailMessage(VerificationEmailRequest request) {
        logger.info("Sending Verification Email message: {}", request);
        rabbitTemplate.convertAndSend(exchange, platformEmailsRoutingKey, request);
    }

    public void sendWelcomeEmailMessage(WelcomeEmailRequest request) {
        logger.info("Sending Welcome Email message: {}", request);
        rabbitTemplate.convertAndSend(exchange, platformEmailsRoutingKey, request);
    }

    public void sendResetPasswordEmailMessage(ResetPasswordEmailRequest request) {
        logger.info("Sending Reset Password Email message: {}", request);
        rabbitTemplate.convertAndSend(exchange, platformEmailsRoutingKey, request);
    }

}
