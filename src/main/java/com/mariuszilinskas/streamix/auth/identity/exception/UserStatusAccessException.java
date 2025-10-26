package com.mariuszilinskas.streamix.auth.identity.exception;

/**
 * This class represents a custom exception to be thrown when
 * user status is not ACTIVE
 *
 * @author Marius Zilinskas
 */
public class UserStatusAccessException extends RuntimeException {

    public UserStatusAccessException(String userStatus) {
        super(String.format("No Access as User Status is '%s'", userStatus));
    }

}
