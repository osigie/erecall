package com.osigie.erecall;

import com.osigie.erecall.config.S3Properties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(S3Properties.class)
public class ErecallApplication {

  public static void main(String[] args) {
    SpringApplication.run(ErecallApplication.class, args);
  }
}
