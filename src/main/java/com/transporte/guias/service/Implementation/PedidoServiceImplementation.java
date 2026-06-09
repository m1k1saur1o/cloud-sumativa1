package com.transporte.guias.service.Implementation;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.transporte.guias.entity.Pedido;
import com.transporte.guias.repository.PedidoRepository;
import com.transporte.guias.service.PedidoService;

@Service
public class PedidoServiceImplementation implements PedidoService {

    @Autowired
    private PedidoRepository pedidoRepository;

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
}
