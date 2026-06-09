package com.transporte.guias.service.Implementation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import com.transporte.guias.service.PedidoStorageService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transporte.guias.dto.GuiaS3Document;
import com.transporte.guias.entity.Pedido;
import com.transporte.guias.repository.PedidoRepository;
import com.transporte.guias.service.PedidoService;

@Service
public class PedidoServiceImplementation implements PedidoService {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private PedidoStorageService pedidoStorageService;

    @Override
    public List<Pedido> findAll() {
        return pedidoRepository.findAll();
    }

    @Override
    public Optional<Pedido> findById(Long id) {
        return pedidoRepository.findById(id);
    }

    @Override
    public Optional<Pedido> findByCliente(String cliente) {
        return pedidoRepository.findByCliente(cliente);
    }

    @Override
    public Pedido save(Pedido pedido) {
        return pedidoRepository.save(pedido);
    }

    @Override
    public void deleteById(Long id) {
        pedidoRepository.deleteById(id);
    }

    @Override
    @Transactional
    public Pedido crearPedido(Pedido pedido) {

        Pedido saved = pedidoRepository.save(pedido);

        LocalDate now = LocalDate.now();

        String yearMonth = now.format(DateTimeFormatter.ofPattern("yyyyM"));

        pedidoStorageService.guardarPedidoJson(
            yearMonth,
            pedido.getTransportista(),
            pedido.getCliente(),
            pedido.getDireccion()
        );
        return saved;
    }
}
