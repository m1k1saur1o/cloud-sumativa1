package com.transporte.guias.dto;

import java.time.LocalDateTime;

public class PedidoResponse {
    private final Long id;
    private final String cliente;
    private final String direccion;
    private final String transportista;
    private final LocalDateTime fecha;

    public PedidoResponse(Long id, String cliente, String direccion, String transportista, LocalDateTime fecha) {
        this.id = id;
        this.cliente = cliente;
        this.direccion = direccion;
        this.transportista = transportista;
        this.fecha = fecha;
    }

    public Long getId() {
        return id;
    }

    public String getCliente() {
        return cliente;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getTransportista() {
        return transportista;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}
