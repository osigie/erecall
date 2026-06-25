package com.osigie.erecall.service;

public interface EmbeddingService {
    /*
     * Generate vector embeddings for sematic search
     * Store in Qdrant*/

    String generateEmbedding(String text);

    void saveEmbedding(String document);
}
