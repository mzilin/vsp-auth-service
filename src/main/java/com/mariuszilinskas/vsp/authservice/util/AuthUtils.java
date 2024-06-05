package com.mariuszilinskas.vsp.authservice.util;

public abstract class AuthUtils {

    private AuthUtils() {
        // Private constructor to prevent instantiation
    }

    public static final String PRODUCTION_ENV = "production";

    public static final String TIMESTAMP_FORMAT = "yyyy-MM-dd hh:mm:ss";

    public static final String ACCESS_TOKEN_NAME = "vsp_access";

    public static final String REFRESH_TOKEN_NAME = "vsp_refresh";

    public static final long FIFTEEN_MINUTES_IN_MILLIS = 15 * 60 * 1000L; // 15 minutes

    public static final long ACCESS_TOKEN_EXPIRATION_MILLIS = FIFTEEN_MINUTES_IN_MILLIS;

    public static final long REFRESH_TOKEN_EXPIRATION_MILLIS = 7 * 24 * 60 * 60 * 1000L; // 7 days

}
