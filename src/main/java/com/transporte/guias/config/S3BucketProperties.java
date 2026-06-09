package com.transporte.guias.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "app.s3")
public class S3BucketProperties {

	private String bucket;
}
