package com.mariuszilinskas.streamix.auth.identity.exception;

public class RefreshTokenValidationException extends RuntimeException {

    public RefreshTokenValidationException(String message) {
        super(message);
    }

}
