package com.transporte.guias.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.service.annotation.DeleteExchange;

import com.transporte.guias.dto.PedidoRequest;
import com.transporte.guias.dto.PedidoResponse;
import com.transporte.guias.entity.Pedido;
import com.transporte.guias.mapper.PedidoMapper;
import com.transporte.guias.service.PedidoService;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {
    @Autowired
    private PedidoService pedidoService;

    @GetMapping
    public List<PedidoResponse> listarPedidos() {
        return pedidoService.findAll()
                .stream()
                .map(PedidoMapper::toDTO)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> obtenerPedidoPorId(@PathVariable Long id) {
        return pedidoService.findById(id)
                .map(PedidoMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public PedidoResponse crearPedido(@RequestBody PedidoRequest dto) {
        Pedido pedido = PedidoMapper.toEntity(dto);
        pedido.setFecha(LocalDate.now());
        Pedido saved = pedidoService.save(pedido);
        return PedidoMapper.toDTO(saved);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<PedidoResponse> actualizarPedido(@PathVariable Long id, @RequestBody PedidoRequest dto) {
        return pedidoService.findById(id)
                .map(pedido -> {
                    pedido.setCliente(dto.getCliente());
                    pedido.setDireccion(dto.getDireccion());
                    pedido.setTransportista(dto.getTransportista());
                    Pedido updated = pedidoService.save(pedido);
                    return ResponseEntity.ok(PedidoMapper.toDTO(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteExchange("/{id}")
    public ResponseEntity<Void> eliminarPedido(@PathVariable Long id) {
        if (pedidoService.findById(id).isPresent()) {
            pedidoService.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
