package com.osigie.erecall.service.impl;

import com.osigie.erecall.domain.DocumentProcessingStatus;
import com.osigie.erecall.domain.entity.ExpenseDocument;
import com.osigie.erecall.domain.entity.User;
import com.osigie.erecall.event.DocumentSavedEvent;
import com.osigie.erecall.exception.ResourceNotFoundException;
import com.osigie.erecall.repo.ExpenseDocumentRepository;
import com.osigie.erecall.repo.UserRepository;
import com.osigie.erecall.security.AuthHelper;
import com.osigie.erecall.service.ExpenseService;
import com.osigie.erecall.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.content.Media;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.net.URI;
import java.time.Duration;
import java.util.List;

@Service
@Slf4j
public class DocumentProcessingService {

    private final ExpenseDocumentRepository expenseDocumentRepository;
    private final ExpenseService expenseService;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

    public DocumentProcessingService(ExpenseDocumentRepository expenseDocumentRepository,
                                     ExpenseService expenseService,
                                     FileStorageService fileStorageService, UserRepository userRepository) {
        this.expenseDocumentRepository = expenseDocumentRepository;
        this.expenseService = expenseService;
        this.fileStorageService = fileStorageService;
        this.userRepository = userRepository;
    }

    @Async("documentProcessingExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDocumentSaved(DocumentSavedEvent event) {
        ExpenseDocument document = expenseDocumentRepository.findByIdWithCreator(event.documentId()).orElse(null);

        if (document == null) {
            log.warn("Document {} not found for processing", event.documentId());
            return;
        }

        User user = document.getCreator();

        try {
            String response;
            if (hasFile(document)) {
                String presignedUrl = fileStorageService.generatePresignedUrl(document.getFileUrl(), Duration.ofMinutes(5));
                Media media = new Media(resolveMimeType(document.getFileUrl()), URI.create(presignedUrl));
                String text = document.getRawText() != null && !document.getRawText().isBlank()
                        ? document.getRawText()
                        : "Extract expense information from this document.";
                response = expenseService.queryWithMedia(
                        text, List.of(media), user, document.getId());
            } else {
                response = expenseService.query(
                        document.getRawText(), user, document.getId());
            }
            updateStatus(document, DocumentProcessingStatus.PROCESSED, response);
        } catch (Exception e) {
            log.error("Failed to process document {}", event.documentId(), e);
            updateStatus(document, DocumentProcessingStatus.FAILED, "Failed to process document");
        }
    }

    private boolean hasFile(ExpenseDocument event) {
        return event.getFileUrl() != null && !event.getFileUrl().isBlank();
    }

    private void updateStatus(ExpenseDocument document, DocumentProcessingStatus status, String response) {
        document.setProcessingStatus(status);
        document.setAiResponse(response);
        expenseDocumentRepository.save(document);
    }

    private MimeType resolveMimeType(String fileUrl) {
        String lower = fileUrl.toLowerCase();
        if (lower.endsWith(".pdf")) return MimeTypeUtils.parseMimeType("application/pdf");
        if (lower.endsWith(".png")) return MimeTypeUtils.IMAGE_PNG;
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MimeTypeUtils.IMAGE_JPEG;
        return MimeTypeUtils.parseMimeType("application/pdf");
    }
}
