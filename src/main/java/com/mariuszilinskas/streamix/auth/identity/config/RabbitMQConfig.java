package com.mariuszilinskas.streamix.auth.identity.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.queues.create-credentials}")
    private String createCredentialsQueue;

    @Value("${rabbitmq.routing-keys.create-credentials}")
    private String createCredentialsRoutingKey;

    @Value("${rabbitmq.queues.reset-passcode}")
    private String resetPasscodeQueue;

    @Value("${rabbitmq.routing-keys.reset-passcode}")
    private String resetPasscodeRoutingKey;

    @Value("${rabbitmq.queues.delete-user-data}")
    private String deleteUserDataQueue;

    @Value("${rabbitmq.routing-keys.delete-user-data}")
    private String deleteUserDataRoutingKey;

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(exchange);
    }

    @Bean
    public Queue createCredentialsQueue() {
        return new Queue(createCredentialsQueue, true);
    }

    @Bean
    public Binding createCredentialsBinding() {
        return BindingBuilder.bind(createCredentialsQueue())
                .to(exchange())
                .with(createCredentialsRoutingKey);
    }

    @Bean
    public Queue resetPasscodeQueue() {
        return new Queue(resetPasscodeQueue, true);
    }

    @Bean
    public Binding resetPasscodeBinding() {
        return BindingBuilder.bind(resetPasscodeQueue())
                .to(exchange())
                .with(resetPasscodeRoutingKey);
    }

    @Bean
    public Queue deleteUserDataQueue() {
        return new Queue(deleteUserDataQueue, true);
    }

    @Bean
    public Binding deleteUserDataBinding() {
        return BindingBuilder.bind(deleteUserDataQueue())
                .to(exchange())
                .with(deleteUserDataRoutingKey);
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jacksonConverter());
        return rabbitTemplate;
    }

    @Bean
    public MessageConverter jacksonConverter() {
        return new Jackson2JsonMessageConverter();
    }

}

