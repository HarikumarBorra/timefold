package com.timefold.common.exception;

public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = -7335814741610035696L;

	public ResourceNotFoundException(String message) {
        super(message);
    }
}