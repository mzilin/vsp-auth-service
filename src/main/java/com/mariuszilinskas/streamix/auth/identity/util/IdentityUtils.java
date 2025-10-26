package com.mariuszilinskas.streamix.auth.identity.util;

import com.mariuszilinskas.streamix.auth.identity.enums.UserStatus;
import com.mariuszilinskas.streamix.auth.identity.exception.UserStatusAccessException;

import java.util.EnumSet;

public abstract class IdentityUtils {

    private IdentityUtils() {
        // Private constructor to prevent instantiation
    }

    public static final String PRODUCTION_ENV = "production";

    public static final String TIMESTAMP_FORMAT = "yyyy-MM-dd hh:mm:ss";

    public static final String ACCESS_TOKEN_NAME = "vsp_access";

    public static final String REFRESH_TOKEN_NAME = "vsp_refresh";

    public static final long FIFTEEN_MINUTES_IN_MILLIS = 15 * 60 * 1000L; // 15 minutes

    public static final long ACCESS_TOKEN_EXPIRATION_MILLIS = FIFTEEN_MINUTES_IN_MILLIS;

    public static final long REFRESH_TOKEN_EXPIRATION_MILLIS = 7 * 24 * 60 * 60 * 1000L; // 7 days

    public static void checkUserSuspended(UserStatus status) {
        if (EnumSet.of(UserStatus.SUSPENDED, UserStatus.LOCKED, UserStatus.INACTIVE).contains(status)) {
            throw new UserStatusAccessException(status.toString());
        }
    }

}
