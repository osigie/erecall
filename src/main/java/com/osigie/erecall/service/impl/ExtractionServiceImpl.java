package com.osigie.erecall.service.impl;

import com.osigie.erecall.domain.valueObjects.ExpenseExtraction;
import com.osigie.erecall.service.ExtractionService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;

public class ExtractionServiceImpl implements ExtractionService {
    private final ChatClient chatClient;

    public ExtractionServiceImpl(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public ExpenseExtraction extract(String text) {

        BeanOutputConverter<ExpenseExtraction> converter = new BeanOutputConverter<>(ExpenseExtraction.class);
        String extractedText = chatClient.prompt()
                .user(u -> u.text("""
                          Extract structured expense information.
                        %s
                        
                        Text: %s
                        """.formatted(converter.getFormat(), text)))
                .call()
                .content();

       //TODO: consider creation exception
        if (extractedText == null) {
            throw new RuntimeException("No extracted structured expense information found.");
        }

        return converter.convert(extractedText);

    }


}
