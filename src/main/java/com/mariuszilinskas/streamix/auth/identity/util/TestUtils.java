package com.mariuszilinskas.streamix.auth.identity.util;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;

import java.util.Collections;
import java.util.UUID;

public abstract class TestUtils {

    private TestUtils() {
        // Private constructor to prevent instantiation
    }

    public static final String secretKey = "ygPv2rQAHiDnm3W1dOvUGKYQsJ9hKyJng9hEk4vaGUuS878jsK+KZRbzV9JEtRoC";

    public static final UUID userId = UUID.fromString("432c6a32-bbbb-4c24-8813-af8172b865f5");

    public static final UUID tokenId = UUID.fromString("0ed2d7b3-03b4-4bd6-92b7-7c67a84b7595");

    public static final String validAccessToken = "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiI0MzJjNmEzMi1iYmJiLTRjMjQtODgxMy1hZjgxNzJiODY1ZjUiLCJpYXQiOjE3MTc1NDM4NzAsImV4cCI6MzI5NTM4MDY3MCwicm9sZXMiOlsiVVNFUiIsIkFETUlOIl0sImF1dGhvcml0aWVzIjpbIk1BTkFHRV9TRVRUSU5HUyJdfQ.ZdGKXj5wnRgBVAMu8_GLSy0jvDSIqDM5h-ORpAZGHpMrUohpb2csqOl9-4U4qo5l";

    public static final String validRefreshToken = "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiI0MzJjNmEzMi1iYmJiLTRjMjQtODgxMy1hZjgxNzJiODY1ZjUiLCJpYXQiOjE3MTc1NDM4NzAsImV4cCI6MzI5NTM4MDY3MCwidG9rZW5JZCI6IjBlZDJkN2IzLTAzYjQtNGJkNi05MmI3LTdjNjdhODRiNzU5NSIsInJvbGVzIjpbIlVTRVIiLCJBRE1JTiJdLCJhdXRob3JpdGllcyI6WyJNQU5BR0VfU0VUVElOR1MiXX0.ADklAyAXLFzDxpjhXjoU45FrfTDGH7YR3chRAkcv2xkedkpzpA2OY2p9BgJSvF0O";

    public static final String expiredAccessToken = "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiI0MzJjNmEzMi1iYmJiLTRjMjQtODgxMy1hZjgxNzJiODY1ZjUiLCJpYXQiOjE3MDg5NzUzNTUsImV4cCI6MTcwODk3NTM1OH0.Unkrb1Y1rmTtRSys821e-JXx3mmtikB6v0brc9AMnt_8P7xyp-vph54e6Om_YlNH";

    public static final String expiredRefreshToken = "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiI0MzJjNmEzMi1iYmJiLTRjMjQtODgxMy1hZjgxNzJiODY1ZjUiLCJpYXQiOjE3MDg5NzUzNTUsImV4cCI6MTcwODk3NTM3MCwidG9rZW5JZCI6IjBlZDJkN2IzLTAzYjQtNGJkNi05MmI3LTdjNjdhODRiNzU5NSJ9.gTE5VlezJ6nNHiB1KS8q2GtjDXF1mI5ETMEBHr8i3HvTdyPlPj1oDo-xyA-coOu9";

    public static final String invalidToken = "invalid.jwt.token";

    public static FeignException createFeignException() {
        Request feignRequest = Request.create(
                Request.HttpMethod.POST,
                "", // Empty string for URL as a placeholder
                Collections.emptyMap(),
                null,
                new RequestTemplate()
        );
        return new FeignException.NotFound("Not found", feignRequest, null, Collections.emptyMap());
    }

}
