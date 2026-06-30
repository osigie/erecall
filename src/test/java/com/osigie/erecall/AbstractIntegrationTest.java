package com.osigie.erecall;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.qdrant.QdrantContainer;
import org.testcontainers.utility.DockerImageName;

public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer POSTGRES_CONTAINER;
    static final QdrantContainer QDRANT_CONTAINER;

    static {
        POSTGRES_CONTAINER = new PostgreSQLContainer(DockerImageName.parse("postgres:latest"))
                .withUsername("username")
                .withPassword("password")
                .withDatabaseName("erecall_db");
        POSTGRES_CONTAINER.start();

        QDRANT_CONTAINER = new QdrantContainer(DockerImageName.parse("qdrant/qdrant:latest"));
        QDRANT_CONTAINER.start();
    }

    @DynamicPropertySource
    static void dynamicPropertySource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
        registry.add("spring.ai.vectorstore.qdrant.host", QDRANT_CONTAINER::getHost);
        registry.add("spring.ai.vectorstore.qdrant.port", () -> String.valueOf(QDRANT_CONTAINER.getGrpcPort()));
    }
}
