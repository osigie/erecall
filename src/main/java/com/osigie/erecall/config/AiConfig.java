package com.osigie.erecall.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {
    private final JdbcChatMemoryRepository jdbcChatMemoryRepository;
    private final VectorStore vectorStore;

    public AiConfig(JdbcChatMemoryRepository jdbcChatMemoryRepository, VectorStore vectorStore) {
        this.jdbcChatMemoryRepository = jdbcChatMemoryRepository;
        this.vectorStore = vectorStore;
    }

    @Bean
    public ChatMemory chatMemory(JdbcChatMemoryRepository jdbcChatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(jdbcChatMemoryRepository).build();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("""
                        You are a financial assistant.
                        Use the tools and context provided to answer questions about expenses.
                        """)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory(jdbcChatMemoryRepository)).build(),
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(SearchRequest.builder().topK(5).build())
                                .build())
                .build();

    }
}
