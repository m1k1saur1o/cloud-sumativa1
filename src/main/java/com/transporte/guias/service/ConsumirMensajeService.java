package com.transporte.guias.service;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transporte.guias.dto.PedidoRequest;
import com.transporte.guias.entity.Pedido;
import com.transporte.guias.service.Implementation.PedidoServiceImplementation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.io.IOException;


@Slf4j
@Service
@RequiredArgsConstructor
public class ConsumirMensajeService {

	private final PedidoServiceImplementation pedidoService;
	private final AwsS3Service awsS3Service;
	private final ObjectMapper objectMapper;

	@RabbitListener(id = "listener-myQueue", queues = "myQueue", ackMode = "MANUAL", containerFactory = "rawRabbitListenerContainerFactory")
	public void recibirMensajeConAckManual(Message mensaje, Channel canal) throws IOException {

		try {

			String body = new String(mensaje.getBody());
			log.info("Mensaje recibido: {}", body);

			// Deserializar JSON a PedidoRequest
			PedidoRequest pedidoRequest = objectMapper.readValue(body, PedidoRequest.class);

            // Validar campos obligatorios del PedidoRequest
			if (pedidoRequest.getCliente() == null || pedidoRequest.getCliente().isBlank()
					|| pedidoRequest.getDireccion() == null || pedidoRequest.getDireccion().isBlank()
					|| pedidoRequest.getTransportista() == null || pedidoRequest.getTransportista().isBlank()) {
				throw new RuntimeException("Error de validación: campos obligatorios faltantes en el mensaje");
			}

			// Crear Pedido y guardarlo en H2
			Pedido pedido = new Pedido();
			pedido.setCliente(pedidoRequest.getCliente());
			pedido.setDireccion(pedidoRequest.getDireccion());
			pedido.setTransportista(pedidoRequest.getTransportista());
			pedido.setFecha(LocalDateTime.now());

			Pedido savedPedido = pedidoService.save(pedido);
			log.info("Pedido guardado en H2 con ID: {}", savedPedido.getId());

			// Subir JSON a S3
			awsS3Service.parsePedidoToJson(pedidoRequest, savedPedido.getId());
			log.info("Pedido JSON subido a S3 para ID: {}", savedPedido.getId());

			canal.basicAck(mensaje.getMessageProperties().getDeliveryTag(), false);
			log.info("Acknowledge OK enviado");
		} catch (Exception e) {
			canal.basicNack(mensaje.getMessageProperties().getDeliveryTag(), false, false);
			log.error("Error al procesar mensaje: {}", e.getMessage(), e);
		}
	}
}
