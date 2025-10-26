package com.mariuszilinskas.streamix.auth.identity.exception;

public class ResourceNotFoundException extends RuntimeException {

    private static final String ERROR_MESSAGE_TEMPLATE = "No %s found with %s = '%s'. Please check the %s and try again.";

    public <T> ResourceNotFoundException(Class<T> entity, String identifierType, Object identifierValue) {
        super(formatErrorMessage(entity.getSimpleName(), identifierType, identifierValue));
    }

    public ResourceNotFoundException(String entity, String identifierType, Object identifierValue) {
        super(formatErrorMessage(entity, identifierType, identifierValue));
    }

    private static String formatErrorMessage(String entity, String identifierType, Object identifierValue) {
        return String.format(ERROR_MESSAGE_TEMPLATE, entity, identifierType, identifierValue, identifierType);
    }

}
