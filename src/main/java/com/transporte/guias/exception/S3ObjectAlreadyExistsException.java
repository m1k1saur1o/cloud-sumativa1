package com.transporte.guias.exception;

public class S3ObjectAlreadyExistsException extends RuntimeException {

	public S3ObjectAlreadyExistsException(String key, String bucketName) {
		super("El objeto '" + key + "' ya existe en el bucket '" + bucketName + "'");
	}
}
