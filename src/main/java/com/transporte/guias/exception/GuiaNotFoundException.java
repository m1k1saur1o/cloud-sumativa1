package com.transporte.guias.exception;

public class GuiaNotFoundException extends RuntimeException {

	public GuiaNotFoundException(Long guiaId) {
		super("La guía con id '" + guiaId + "' no existe");
	}
}
