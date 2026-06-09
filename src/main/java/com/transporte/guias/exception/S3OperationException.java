package com.transporte.guias.exception;
public class S3OperationException extends RuntimeException {

	public S3OperationException(String message) {
		super(message);
	}

	public S3OperationException(String message, Throwable cause) {
		super(message, cause);
	}
}
