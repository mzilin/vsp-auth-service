package com.mariuszilinskas.streamix.auth.identity.exception;

/**
 * This class represents a custom exception to be thrown when
 * Jwt Token generation fails
 *
 * @author Marius Zilinskas
 */
public class JwtTokenGenerationException extends RuntimeException {

    public JwtTokenGenerationException(String tokenType) {
        super(String.format("Error when generating %s.", tokenType));
    }

}
