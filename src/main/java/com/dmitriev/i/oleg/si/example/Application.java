package com.dmitriev.i.oleg.si.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.integration.config.EnableIntegrationManagement;

@SpringBootApplication
@EnableIntegrationManagement(observationPatterns = "*")
@ConfigurationPropertiesScan("com.dmitriev.i.oleg.si.example.config.props")
public class Application {
	static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
