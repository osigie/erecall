package com.osigie.erecall.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.qdrant.QdrantContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgreSQLContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:9.6.12"))
                .withUsername("username")
                .withPassword("password")
                .withDatabaseName("erecall_db");
    }


    @Bean
    @ServiceConnection
    QdrantContainer qdrantContainer() {
        return new QdrantContainer(DockerImageName.parse("qdrant/qdrant:v1.7.4"));

    }


}
