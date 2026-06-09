package com.transporte.guias.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.transporte.guias.service.PedidoStorageService;


@RestController
@RequestMapping("/s3")
public class S3PedidoController {
    private final PedidoStorageService pedidoStorageService;

    public S3PedidoController(PedidoStorageService pedidoStorageService) {
        this.pedidoStorageService = pedidoStorageService;
    }

    @GetMapping(value = "/guias/{fecha}/{transportista}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> downloadSummary(@PathVariable String fecha, @PathVariable String transportista) {
        String payload = pedidoStorageService.descargarGuia(fecha, transportista);
        return ResponseEntity.ok(payload);
    }

    @PutMapping(value = "/guias/{fecha}/{transportista}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> actualizarGuia(
            @PathVariable String fecha,
            @PathVariable String transportista,
            @RequestBody String payload
    ) {
        pedidoStorageService.actualizarGuia(fecha, transportista, payload);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/guias/{fecha}/{transportista}")
    public ResponseEntity<Void> deleteSummary(@PathVariable String fecha, @PathVariable String transportista) {
        pedidoStorageService.eliminarGuia(fecha, transportista);
        return ResponseEntity.noContent().build();
    }
}
