package com.osigie.erecall.service.impl;

import com.osigie.erecall.domain.DocumentProcessingStatus;
import com.osigie.erecall.domain.entity.ExpenseDocument;
import com.osigie.erecall.event.DocumentSavedEvent;
import com.osigie.erecall.repo.ExpenseDocumentRepository;
import com.osigie.erecall.service.ExpenseService;
import com.osigie.erecall.service.FileExtractionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@Slf4j
public class DocumentProcessingService {


    private final ExpenseDocumentRepository expenseDocumentRepository;
    private final FileExtractionService fileExtractionService;
    private final ExpenseService expenseService;

    public DocumentProcessingService(ExpenseDocumentRepository expenseDocumentRepository,
                                     FileExtractionService fileExtractionService,
                                     ExpenseService expenseService) {
        this.expenseDocumentRepository = expenseDocumentRepository;
        this.fileExtractionService = fileExtractionService;
        this.expenseService = expenseService;
    }

    @Async("documentProcessingExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDocumentSaved(DocumentSavedEvent event) {
        ExpenseDocument document = expenseDocumentRepository.findById(event.documentId()).orElse(null);
        if (document == null) {
            log.warn("Document {} not found for processing", event.documentId());
            return;
        }

        try {
            updateStatus(document, DocumentProcessingStatus.PROCESSING);
            String text = fileExtractionService.extractText(document.getFileUrl());
            expenseService.query(text, document.getCreator().getId(), document.getId());
            updateStatus(document, DocumentProcessingStatus.PROCESSED);
        } catch (Exception e) {
            log.error("Failed to process document {}", event.documentId(), e);
            updateStatus(document, DocumentProcessingStatus.FAILED);
        }
    }

    private void updateStatus(ExpenseDocument document, DocumentProcessingStatus status) {
        document.setProcessingStatus(status);
        expenseDocumentRepository.save(document);
    }
}
