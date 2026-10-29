package com.example.elibrary.domain.exception;

/** Requested aggregate does not exist (or is not visible to the caller). */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resourceType, Object identifier) {
        super(BusinessErrorCode.RESOURCE_NOT_FOUND, resourceType + " not found: " + identifier);
    }

    public ResourceNotFoundException(String message) {
        super(BusinessErrorCode.RESOURCE_NOT_FOUND, message);
    }
}
