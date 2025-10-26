package com.mariuszilinskas.streamix.auth.identity.exception;

public class ResetTokenValidationException extends RuntimeException {

    public ResetTokenValidationException() {
        super("Reset Token doesn't exist or has expired.");
    }

}
