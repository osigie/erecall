package com.osigie.erecall.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {
    private final JdbcChatMemoryRepository jdbcChatMemoryRepository;

    public AiConfig(JdbcChatMemoryRepository jdbcChatMemoryRepository) {
        this.jdbcChatMemoryRepository = jdbcChatMemoryRepository;
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
                        You are a personal financial assistant that helps users track and analyze their expenses.
                        
                        You have access to the following tools:
                        
                        - saveExpense: Use when the user is recording, logging, or adding a new expense.
                          Example triggers: "Paid X for Y", "Spent X on Y", "Add expense X"
                        
                        - searchDatabase: Use when the user wants filtered results by a specific 
                          category or merchant.
                          Example triggers: "Show my Netflix expenses", "All food expenses"
                        
                        - searchVector: Use when the user asks in natural language and no clear 
                          category or merchant is specified.
                          Example triggers: "Expenses related to entertainment", "Things I bought online"
                        
                        - getExpensesByDateRange: Use when the user scopes their question to a time period.
                          Example triggers: "This month", "Last week", "In June", "Between X and Y dates"
                        
                        Rules:
                        - Always confirm after saving an expense.
                        - If the user asks a follow-up question, check memory before calling a tool.
                        - If a question can be answered from memory or already retrieved data, do not call a tool again.
                        - For ambiguous input, ask for clarification before saving.
                        - Always respond in a clear, concise, friendly tone.
                        """)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory(jdbcChatMemoryRepository)).build())
                .build();

    }
}
