package com.transporte.guias.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transporte.guias.config.S3EnrollmentProperties;
import com.transporte.guias.dto.GuiaS3Document;
import com.transporte.guias.entity.Pedido;

import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Template;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import com.transporte.guias.exception.EnrollmentStorageException;
import com.transporte.guias.exception.NotFoundException; 
import com.transporte.guias.exception.BadRequestException;
import com.fasterxml.jackson.core.JsonProcessingException;


@Service
public class PedidoStorageService {
    private static final Logger log = LoggerFactory.getLogger(PedidoStorageService.class);
    private static final String ENROLLMENTS_PREFIX = "enrollments";
    private static final String JSON_CONTENT_TYPE = "application/json";

    private final S3Template s3Template;
    private final ObjectMapper objectMapper;
    private final S3EnrollmentProperties properties;
    private final S3Client s3Client;

    public PedidoStorageService(
            S3Template s3Template,
            ObjectMapper objectMapper,
            S3EnrollmentProperties properties,
            S3Client s3Client
    ) {
        this.s3Template = s3Template;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.s3Client = s3Client;
    }

    public void guardarGuia(String fecha, String transportista, GuiaS3Document document) {

        String key = buildKey(fecha, transportista);
        byte[] payload;
        try {
            payload = objectMapper.writeValueAsBytes(document);
        } catch (JsonProcessingException ex) {
            throw new EnrollmentStorageException("Failed to serialize enrollment payload", ex);
        }

        uploadPayload(key, payload);
    }

    public String descargarGuia(String fecha, String transportista) {
        String key = buildKey(fecha, transportista);
        if (!objectExists(key)) {
            throw new NotFoundException("Enrollment summary not found: " + key);
        }

        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .build();
            ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObjectAsBytes(request);
            return responseBytes.asUtf8String();
        } catch (NoSuchKeyException ex) {
            throw new NotFoundException("Enrollment summary not found: " + key);
        } catch (S3Exception ex) {
            throw new EnrollmentStorageException("Failed to download enrollment summary from S3", ex);
        }
    }

    public void actualizarGuia(String fecha, String transportista, String jsonPayload) {
        String key = buildKey(fecha, transportista);
        if (!objectExists(key)) {
            throw new NotFoundException("Enrollment summary not found: " + key);
        }

        byte[] payloadBytes = jsonPayload.getBytes(StandardCharsets.UTF_8);
        uploadPayload(key, payloadBytes);
    }

    public void eliminarGuia(String fecha, String transportista) {
        String key = buildKey(fecha, transportista);

        if (!objectExists(key)) {
            throw new NotFoundException("Enrollment summary not found: " + key);
        }

        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .build();
            s3Client.deleteObject(request);
        } catch (S3Exception ex) {
            throw new EnrollmentStorageException("Failed to delete enrollment summary from S3", ex);
        }
    }

    private void uploadPayload(String key, byte[] payload) {
        ObjectMetadata metadata = ObjectMetadata.builder()
                .contentType(JSON_CONTENT_TYPE)
                .contentLength((long) payload.length)
                .build();

        ByteArrayInputStream inputStream = new ByteArrayInputStream(payload);
        try {
            s3Template.upload(properties.getBucket(), key, inputStream, metadata);
            log.info("Enrollment summary stored in S3: bucket={}, key={}", properties.getBucket(), key);
        } catch (RuntimeException ex) {
            throw new EnrollmentStorageException("Failed to upload enrollment to S3", ex);
        }
    }

    private boolean objectExists(String key) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .build();
            s3Client.headObject(request);
            return true;
        } catch (NoSuchKeyException ex) {
            return false;
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404) {
                return false;
            }
            throw new EnrollmentStorageException("Failed to check S3 object existence", ex);
        }
    }

    private String buildKey(String fecha, String transportista) {
        return ENROLLMENTS_PREFIX + "/" + fecha + "/" + transportista + ".json";
    }

    public void guardarPedidoJson(String fecha, String transportista, Pedido pedido) {

        String key = buildKey(fecha, transportista);

         try {
            byte[] payload = objectMapper.writeValueAsBytes(pedido);

            uploadPayload(key, payload);

        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Error serializando Pedido a JSON", ex);
        }
    }

}
