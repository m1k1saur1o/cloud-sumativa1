package com.transporte.guias.dto;

public class PedidoResponse {
    private final Long id;
    private final String cliente;
    private final String direccion;
    private final String transportista;

    public PedidoResponse(Long id, String cliente, String direccion, String transportista) {
        this.id = id;
        this.cliente = cliente;
        this.direccion = direccion;
        this.transportista = transportista;
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
}
