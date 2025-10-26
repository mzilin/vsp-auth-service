package com.mariuszilinskas.streamix.auth.identity.exception;

public class EmailVerificationException extends RuntimeException {

    public EmailVerificationException() {
        super("Email verification failed. Try again later.");
    }

}
