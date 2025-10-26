package com.mariuszilinskas.streamix.auth.identity.exception;

public class PasscodeExpiredException extends RuntimeException {

    public PasscodeExpiredException() {
        super("This passcode has expired.");
    }

}
