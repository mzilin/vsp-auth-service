package com.mariuszilinskas.streamix.auth.identity.exception;

public class NoAccessException extends RuntimeException {

    public NoAccessException() {
        super("This request is forbidden");
    }

}
