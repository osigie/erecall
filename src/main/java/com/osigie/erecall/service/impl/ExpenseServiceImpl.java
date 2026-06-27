package com.osigie.erecall.service.impl;

import com.osigie.erecall.domain.DocumentProcessingStatus;
import com.osigie.erecall.domain.entity.ExpenseDocument;
import com.osigie.erecall.domain.entity.User;
import com.osigie.erecall.dto.ExpenseDTO.*;
import com.osigie.erecall.event.DocumentSavedEvent;
import com.osigie.erecall.exception.BadRequestException;
import com.osigie.erecall.exception.ResourceNotFoundException;
import com.osigie.erecall.repo.ExpenseDocumentRepository;
import com.osigie.erecall.repo.ExpenseRepository;
import com.osigie.erecall.service.ExpenseService;
import com.osigie.erecall.service.ExpenseTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class ExpenseServiceImpl implements ExpenseService {

    private final ChatClient chatClient;
    private final ExpenseTools tools;
    private final ExpenseDocumentRepository expenseDocumentRepository;
    private final ExpenseRepository expenseRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ExpenseServiceImpl(ChatClient chatClient, ExpenseTools tools,
                              ExpenseDocumentRepository expenseDocumentRepository,
                              ExpenseRepository expenseRepository,
                              ApplicationEventPublisher eventPublisher) {
        this.chatClient = chatClient;
        this.tools = tools;
        this.expenseDocumentRepository = expenseDocumentRepository;
        this.expenseRepository = expenseRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public String query(String text, UUID userId, UUID documentId) {
        return chatClient.prompt()
                .user(text)
                .system(s -> s.params(Map.of("currentDateTime", LocalDateTime.now().toString())))
                .advisors(a -> a.params(Map.of(ChatMemory.CONVERSATION_ID, userId.toString())))
                .tools(tools)
                .toolContext(Map.of("documentId", documentId, "userId", userId))
                .call()
                .content();
    }

    @Override
    @Transactional
    public SubmitResponse saveExpenseDocument(ExpenseDocument document) {
        document = expenseDocumentRepository.save(document);

        return switch (document.getType()) {
            case TEXT -> emit(document, DocumentProcessingStatus.PROCESSING);
            case PDF -> emit(document, DocumentProcessingStatus.PENDING);
            default -> throw new BadRequestException("Document type not supported: " + document.getType());
        };
    }

    private SubmitResponse emit(ExpenseDocument document, DocumentProcessingStatus status) {
        document.setProcessingStatus(status);
        expenseDocumentRepository.save(document);
        eventPublisher.publishEvent(new DocumentSavedEvent(document.getId(), document.getCreator().getId()));
        return new SubmitResponse(document.getId(), status, null);
    }

    @Override
    public StatusResponse getDocumentStatus(UUID documentId, User user) {
        ExpenseDocument document = expenseDocumentRepository.findByIdAndCreator(documentId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        ExpenseData expenseData = document.getProcessingStatus() == DocumentProcessingStatus.PROCESSED
                ? expenseRepository.findByExpenseDocumentId(documentId)
                  .map(ExpenseData::from)
                  .orElse(null)
                : null;

        return new StatusResponse(document.getId(), document.getProcessingStatus(), document.getAiResponse(), expenseData);
    }
}
