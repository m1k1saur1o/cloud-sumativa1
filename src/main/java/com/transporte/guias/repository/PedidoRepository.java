package com.transporte.guias.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.transporte.guias.entity.Pedido;


public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    Optional<Pedido> findByCliente(String cliente);
    boolean existsByCliente(String cliente);

    @Query("SELECT p FROM Pedido p WHERE DATE(p.fecha) = :date")
    List<Pedido> findByFecha(@Param("date") LocalDate date);

    @Query("SELECT p FROM Pedido p WHERE p.transportista = :transportista")
    List<Pedido> findByTransportista(@Param("transportista") String transportista);

    @Query("SELECT p FROM Pedido p WHERE DATE(p.fecha) = :date AND p.transportista = :transportista")
    List<Pedido> findByFechaYTransportista(@Param("date") LocalDate date, @Param("transportista") String transportista);
}
