package com.transporte.guias.mapper;

import com.transporte.guias.dto.PedidoRequest;
import com.transporte.guias.dto.PedidoResponse;
import com.transporte.guias.entity.Pedido;

public class PedidoMapper {

    public static Pedido toEntity(PedidoRequest dto) {
        Pedido p = new Pedido();
        p.setCliente(dto.getCliente());
        p.setDireccion(dto.getDireccion());
        p.setTransportista(dto.getTransportista());
        p.setFecha(java.time.LocalDateTime.now());
        return p;
    }

    public static PedidoResponse toDTO(Pedido pedido) {
        return new PedidoResponse(
                pedido.getId(),
                pedido.getCliente(),
                pedido.getDireccion(),
                pedido.getTransportista(),
                pedido.getFecha()
        );
    }
}