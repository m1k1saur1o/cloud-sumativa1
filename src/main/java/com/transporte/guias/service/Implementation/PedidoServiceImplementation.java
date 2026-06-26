package com.transporte.guias.service.Implementation;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.transporte.guias.entity.Pedido;
import com.transporte.guias.repository.PedidoRepository;
import com.transporte.guias.service.AwsS3Service;
import com.transporte.guias.service.PedidoService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PedidoServiceImplementation implements PedidoService {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private AwsS3Service awsS3Service;

    @Value("${app.s3.bucket}")
    private String s3Bucket;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
        Pedido saved = pedidoRepository.save(pedido);
        backupToS3(saved);
        return saved;
    }

    @Override
    public void deleteById(Long id) {
        pedidoRepository.deleteById(id);
    }

    /**
     * Guarda una copia de respaldo del pedido en S3 con la ruta:
     * YYYYM/NombreDelTransportista/guia.json
     * Ejemplo: 20266/TransporteExpress/guia.json
     */
    private void backupToS3(Pedido pedido) {
        try {
            String json = objectMapper.writeValueAsString(pedido);
            MultipartFile file = new SimpleMultipartFile(
                    "guia.json".getBytes(), "guia.json", "application/json");

            LocalDate now = LocalDate.now();
            String yearMonth = String.format("%d%d", now.getYear(), now.getMonthValue());
            String key = yearMonth + "/" + pedido.getTransportista() + "/guia.json";

            awsS3Service.upload(s3Bucket, key, file);
            log.info("Respaldo de guia guardado en S3: s3://{}/{}", s3Bucket, key);

        } catch (JsonProcessingException | IOException e) {
            log.error("Error al guardar respaldo en S3 para pedido id={}: {}", pedido.getId(), e.getMessage(), e);
        }
    }

    /**
     * Helper para crear un MultipartFile desde un byte array.
     */
    private static class SimpleMultipartFile implements MultipartFile {
        private final byte[] content;
        private final String filename;
        private final String contentType;

        SimpleMultipartFile(byte[] content, String filename, String contentType) {
            this.content = content;
            this.filename = filename;
            this.contentType = contentType;
        }

        @Override public String getName() { return filename; }
        @Override public String getOriginalFilename() { return filename; }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return content == null || content.length == 0; }
        @Override public long getSize() { return content.length; }
        @Override public byte[] getBytes() { return content; }
        @Override public java.io.InputStream getInputStream() { return new java.io.ByteArrayInputStream(content); }
        @Override public boolean transferTo(java.io.File dest) { throw new UnsupportedOperationException(); }
        @Override public void setMetadata(String name, String value) { throw new UnsupportedOperationException(); }
        @Override public String getMetadata(String name) { throw new UnsupportedOperationException(); }
    }
}
