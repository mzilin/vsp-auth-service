package com.mariuszilinskas.streamix.auth.identity.exception;

import feign.FeignException;

public class FeignClientException extends RuntimeException {

    public FeignClientException(String email, FeignException ex) {
        super(String.format("Feign Exception when getting auth details for email '%s': Status %s, Body %s",
                email, ex.status(), ex.contentUTF8()));
    }

}
