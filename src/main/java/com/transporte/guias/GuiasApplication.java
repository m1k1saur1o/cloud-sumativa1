package com.transporte.guias;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.transporte.guias.config.S3BucketProperties;


//test
@SpringBootApplication
@EnableConfigurationProperties(S3BucketProperties.class)
public class GuiasApplication {

	public static void main(String[] args) {
		SpringApplication.run(GuiasApplication.class, args);
	}

}
