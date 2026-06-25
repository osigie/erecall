package com.osigie.erecall.service.impl;

import com.osigie.erecall.service.EmbeddingService;
import com.osigie.erecall.service.ExpenseService;
import com.osigie.erecall.service.ExtractionService;
import com.osigie.erecall.service.FileExtractionService;

import java.util.UUID;

public class ExpenseServiceImpl implements ExpenseService {
    private final ExtractionService extractionService;
    private final FileExtractionService fileExtractionService;
    private final EmbeddingService embeddingService;

    public ExpenseServiceImpl(ExtractionService extractionService, FileExtractionService fileExtractionService, EmbeddingService embeddingService) {
        this.extractionService = extractionService;
        this.fileExtractionService = fileExtractionService;
        this.embeddingService = embeddingService;
    }

    @Override
    public String query(String text, UUID userId) {
        /*
         * This service to handle all query
         * where user adding expenses and also asking about their expenses*/
        return "";
    }
}
