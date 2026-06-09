package com.transporte.guias.exception;

public class EnrollmentStorageException extends RuntimeException {

    public EnrollmentStorageException(String message) {
        super(message);
    }

    public EnrollmentStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
