package com.transporte.guias.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.transporte.guias.entity.Pedido;


public interface PedidoService {
    List<Pedido> findAll();
    Optional<Pedido> findById(Long id);
    Optional<Pedido> findByCliente(String cliente);
    Pedido save(Pedido pedido);
    void deleteById(Long id);
    List<Pedido> findByFecha(LocalDate fecha);
    List<Pedido> findByTransportista(String transportista);
    List<Pedido> findByFechaYTransportista(LocalDate fecha, String transportista);
}
