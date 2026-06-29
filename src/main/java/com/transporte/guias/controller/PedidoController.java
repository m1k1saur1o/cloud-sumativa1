package com.transporte.guias.controller;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import com.transporte.guias.dto.PedidoRequest;
import com.transporte.guias.dto.PedidoResponse;
import com.transporte.guias.entity.Pedido;
import com.transporte.guias.mapper.PedidoMapper;
import com.transporte.guias.service.AwsS3Service;
import com.transporte.guias.service.PedidoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    @Autowired
    private final PedidoService pedidoService;
    
    @Autowired
    private final AwsS3Service awsS3Service;


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
    public ResponseEntity<PedidoResponse> crearPedido(@RequestBody PedidoRequest dto) {
        Pedido pedido = PedidoMapper.toEntity(dto);
        pedido.setFecha(LocalDateTime.now());
        Pedido saved = pedidoService.save(pedido);

        // Subir respaldo JSON a S3 solo si el guardado fue exitoso
        awsS3Service.parsePedidoToJson(dto, saved.getId());

        PedidoResponse response = PedidoMapper.toDTO(saved);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<PedidoResponse> actualizarPedido(@PathVariable Long id, @RequestBody PedidoRequest dto) {
        return pedidoService.findById(id)
                .map(pedido -> {
                    pedido.setCliente(dto.getCliente());
                    pedido.setDireccion(dto.getDireccion());
                    pedido.setTransportista(dto.getTransportista());
                    Pedido saved = pedidoService.save(pedido);

                    // Actualizar JSON en S3 con la misma key que se usó al crear
                    awsS3Service.parsePedidoToJson(dto, saved.getId());

                    return ResponseEntity.ok(PedidoMapper.toDTO(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPedido(@PathVariable Long id) {
        if (pedidoService.findById(id).isPresent()) {
            pedidoService.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Consulta pedidos por fecha
     * GET /api/pedidos/search/fecha?fecha=2026-06-29
     */
    @GetMapping("/search/fecha")
    public List<PedidoResponse> buscarPorFecha(@RequestParam LocalDate fecha) {
        return pedidoService.findByFecha(fecha)
                .stream()
                .map(PedidoMapper::toDTO)
                .toList();
    }

    /**
     * Consulta pedidos por transportista
     * GET /api/pedidos/search/transportista?transportista=t1
     */
    @GetMapping("/search/transportista")
    public List<PedidoResponse> buscarPorTransportista(@RequestParam String transportista) {
        return pedidoService.findByTransportista(transportista)
                .stream()
                .map(PedidoMapper::toDTO)
                .toList();
    }

    /**
     * Consulta pedidos por fecha y transportista
     * GET /api/pedidos/search?fecha=2026-06-29&transportista=t1
     */
    @GetMapping("/search")
    public List<PedidoResponse> buscarPorFechaYTransportista(
            @RequestParam(required = false) LocalDate fecha,
            @RequestParam(required = false) String transportista) {

        if (fecha != null && transportista != null) {
            return pedidoService.findByFechaYTransportista(fecha, transportista)
                    .stream()
                    .map(PedidoMapper::toDTO)
                    .toList();
        } else if (fecha != null) {
            return pedidoService.findByFecha(fecha)
                    .stream()
                    .map(PedidoMapper::toDTO)
                    .toList();
        } else if (transportista != null) {
            return pedidoService.findByTransportista(transportista)
                    .stream()
                    .map(PedidoMapper::toDTO)
                    .toList();
        } else {
            return pedidoService.findAll()
                    .stream()
                    .map(PedidoMapper::toDTO)
                    .toList();
        }
    }
}
