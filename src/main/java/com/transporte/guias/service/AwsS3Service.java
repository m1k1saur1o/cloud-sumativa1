package com.transporte.guias.service;

import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import com.transporte.guias.dto.S3ObjectDto;
import com.transporte.guias.exception.S3AccessDeniedException;
import com.transporte.guias.exception.S3BucketNotFoundException;
import com.transporte.guias.exception.S3ObjectNotFoundException;
import com.transporte.guias.exception.S3OperationException;
import com.transporte.guias.exception.S3UploadException;
import com.transporte.guias.dto.PedidoRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transporte.guias.config.S3BucketProperties;

@Slf4j
@Service
@RequiredArgsConstructor
public class AwsS3Service {

	private final S3Client s3Client;
	private final ObjectMapper objectMapper;
    private final S3BucketProperties s3BucketProperties;
	

	/**
	 * Lista todos los objetos en un bucket de S3
	 * 
	 * @param bucket Nombre del bucket
	 * @return Lista de objetos en el bucket
	 * @throws S3BucketNotFoundException si el bucket no existe
	 * @throws S3AccessDeniedException   si no hay permisos para listar objetos
	 */
	public List<S3ObjectDto> listObjects(String bucket) {

		try {
			log.info("Listando objetos del bucket: {}", bucket);

			ListObjectsV2Request request = ListObjectsV2Request.builder().bucket(bucket).build();

			ListObjectsV2Response response = s3Client.listObjectsV2(request);

			log.info("Se encontraron {} objetos en el bucket {}", response.contents().size(), bucket);

			return response.contents().stream()
					.map(obj -> new S3ObjectDto(obj.key(), obj.size(),
							obj.lastModified() != null ? obj.lastModified().toString() : null))
					.collect(Collectors.toList());

		} catch (NoSuchBucketException e) {
			throw new S3BucketNotFoundException(bucket, e);
		} catch (S3Exception e) {
			if (e.statusCode() == 403) {
				throw new S3AccessDeniedException("listar objetos del bucket: " + bucket, e);
			}
			throw new S3OperationException("Error al listar objetos del bucket: " + bucket, e);
		}
	}

	/**
	 * Descarga un objeto de S3 como array de bytes
	 * 
	 * @param bucket Nombre del bucket
	 * @param key    Clave del objeto
	 * @return Array de bytes del objeto
	 * @throws S3BucketNotFoundException si el bucket no existe
	 * @throws S3ObjectNotFoundException si el objeto no existe
	 * @throws S3AccessDeniedException   si no hay permisos para descargar
	 */
	public byte[] downloadAsBytes(String bucket, String key) {

		try {
			log.info("Descargando objeto: {} del bucket: {}", key, bucket);

			GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(bucket).key(key).build();

			ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObjectAsBytes(getObjectRequest);

			log.info("Objeto descargado exitosamente: {}", key);

			return responseBytes.asByteArray();

		} catch (NoSuchBucketException e) {
			throw new S3BucketNotFoundException(bucket, e);
		} catch (NoSuchKeyException e) {
			throw new S3ObjectNotFoundException(key, bucket, e);
		} catch (S3Exception e) {
			if (e.statusCode() == 403) {
				throw new S3AccessDeniedException("descargar el objeto: " + key, e);
			}
			throw new S3OperationException("Error al descargar el objeto: " + key, e);
		}
	}

	/**
	 * Sube contenido (por ejemplo JSON serializado) a S3
	 *
	 * @param bucket Nombre del bucket
	 * @param key    Clave del objeto
	 * @param content Contenido del archivo como byte array
	 * @param contentType Tipo de contenido MIME (ej. "application/json")
	 * @throws IllegalArgumentException      si el contenido es nulo o vacío
	 * @throws S3BucketNotFoundException si el bucket no existe
	 * @throws S3AccessDeniedException   si no hay permisos para subir
	 * @throws S3UploadException         si hay error al subir el archivo
	 */
	public void upload(String bucket, String key, byte[] content, String contentType) {

		// Validar que el contenido no sea nulo ni vacío
		if (content == null || content.length == 0) {
			throw new IllegalArgumentException("El contenido a subir no puede ser nulo o vacío");
		}

		try {
			log.info("Subiendo contenido de {} bytes al bucket: {}, key: {}", content.length, bucket, key);

			PutObjectRequest putObjectRequest = PutObjectRequest.builder()
					.bucket(bucket)
					.key(key)
					.contentType(contentType)
					.contentLength((long) content.length)
					.build();

			s3Client.putObject(putObjectRequest, RequestBody.fromBytes(content));

			log.info("Contenido subido exitosamente a: {}", key);

		} catch (NoSuchBucketException e) {
			throw new S3BucketNotFoundException(bucket, e);
		} catch (S3Exception e) {
			if (e.statusCode() == 403) {
				throw new S3AccessDeniedException("subir contenido al bucket: " + bucket, e);
			}
			throw new S3UploadException("Error al subir el contenido a S3: " + e.getMessage(), e);
		}
	}

	/**
	 * Mueve un objeto dentro del mismo bucket (copiar + borrar)
	 * 
	 * @param bucket    Nombre del bucket
	 * @param sourceKey Clave del objeto origen
	 * @param destKey   Clave del objeto destino
	 * @throws S3BucketNotFoundException si el bucket no existe
	 * @throws S3ObjectNotFoundException si el objeto origen no existe
	 * @throws S3AccessDeniedException   si no hay permisos
	 */
	public void moveObject(String bucket, String sourceKey, String destKey) {

		try {
			log.info("Moviendo objeto de {} a {} en el bucket: {}", sourceKey, destKey, bucket);

			CopyObjectRequest copyRequest = CopyObjectRequest.builder().sourceBucket(bucket).sourceKey(sourceKey)
					.destinationBucket(bucket).destinationKey(destKey).build();

			s3Client.copyObject(copyRequest);

			log.info("Objeto copiado exitosamente, procediendo a eliminar el origen");

			deleteObject(bucket, sourceKey);

			log.info("Objeto movido exitosamente de {} a {}", sourceKey, destKey);

		} catch (NoSuchBucketException e) {
			throw new S3BucketNotFoundException(bucket, e);
		} catch (NoSuchKeyException e) {
			throw new S3ObjectNotFoundException(sourceKey, bucket, e);
		} catch (S3Exception e) {
			if (e.statusCode() == 403) {
				throw new S3AccessDeniedException("mover objeto en el bucket: " + bucket, e);
			}
			throw new S3OperationException("Error al mover el objeto de " + sourceKey + " a " + destKey, e);
		}
	}

	/**
	 * Elimina un objeto de S3
	 * 
	 * @param bucket Nombre del bucket
	 * @param key    Clave del objeto
	 * @throws S3BucketNotFoundException si el bucket no existe
	 * @throws S3AccessDeniedException   si no hay permisos para eliminar
	 */
	public void deleteObject(String bucket, String key) {

		try {
			log.info("Eliminando objeto: {} del bucket: {}", key, bucket);

			DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder().bucket(bucket).key(key).build();

			s3Client.deleteObject(deleteRequest);

			log.info("Objeto eliminado exitosamente: {}", key);

		} catch (NoSuchBucketException e) {
			throw new S3BucketNotFoundException(bucket, e);
		} catch (S3Exception e) {
			if (e.statusCode() == 403) {
				throw new S3AccessDeniedException("eliminar objeto del bucket: " + bucket, e);
			}
			throw new S3OperationException("Error al eliminar el objeto: " + key, e);
		}
	}

	/**
     * Serializa el PedidoRequest a JSON y lo sube al bucket S3.
     * La key se construye como: yyyyM/transportista/{id}.json
     */
    public void parsePedidoToJson(PedidoRequest request, Long pedidoId) {
        try {
            // Serializar PedidoRequest a JSON
            String jsonContent = objectMapper.writeValueAsString(request);

            // Construir el path (key) en S3
            LocalDateTime now = LocalDateTime.now();
            String year = String.valueOf(now.getYear());
            int month = now.getMonthValue();
            String transportista = request.getTransportista() != null ? request.getTransportista() : "unknown";
            String key = year + month + "/" + transportista + "/" + pedidoId + ".json";

            // Subir a S3 como application/json
            byte[] contentBytes = jsonContent.getBytes(StandardCharsets.UTF_8);
            upload(s3BucketProperties.getBucket(), key, contentBytes, "application/json");

        } catch (Exception e) {
            // Loguear el error pero no propagarlo para no romper el flujo del pedido
            // El pedido ya fue guardado exitosamente en la BD
        }
    }
}