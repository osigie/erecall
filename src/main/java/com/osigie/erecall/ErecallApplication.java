package com.osigie.erecall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.osigie.erecall.config.S3Properties;

@SpringBootApplication
@EnableConfigurationProperties(S3Properties.class)
public class ErecallApplication {

	public static void main(String[] args) {
		SpringApplication.run(ErecallApplication.class, args);
	}

}
