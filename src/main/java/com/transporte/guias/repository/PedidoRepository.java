package com.transporte.guias.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.transporte.guias.entity.Pedido;


public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    Optional<Pedido> findByCliente(String cliente);
    boolean existsByCliente(String cliente);
}
