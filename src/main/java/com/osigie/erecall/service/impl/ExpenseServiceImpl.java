package com.osigie.erecall.service.impl;

import com.osigie.erecall.domain.DocumentProcessingStatus;
import com.osigie.erecall.domain.entity.ExpenseDocument;
import com.osigie.erecall.domain.entity.User;
import com.osigie.erecall.dto.ExpenseDTO.*;
import com.osigie.erecall.event.DocumentSavedEvent;
import com.osigie.erecall.exception.BadRequestException;
import com.osigie.erecall.exception.ResourceNotFoundException;
import com.osigie.erecall.repo.ExpenseDocumentRepository;
import com.osigie.erecall.repo.projection.DocumentExpenseProjection;
import com.osigie.erecall.service.ExpenseService;
import com.osigie.erecall.service.ExpenseToolService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.content.Media;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class ExpenseServiceImpl implements ExpenseService {

  private final ChatClient chatClient;
  private final ExpenseToolService tools;
  private final ExpenseDocumentRepository expenseDocumentRepository;
  private final ApplicationEventPublisher eventPublisher;

  public ExpenseServiceImpl(
      ChatClient chatClient,
      ExpenseToolService tools,
      ExpenseDocumentRepository expenseDocumentRepository,
      ApplicationEventPublisher eventPublisher) {
    this.chatClient = chatClient;
    this.tools = tools;
    this.expenseDocumentRepository = expenseDocumentRepository;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public String query(String text, User user, UUID documentId) {
    return chatClient
        .prompt()
        .user(text)
        .system(
            s ->
                s.params(
                    Map.of(
                        "currentDateTime",
                        LocalDateTime.now().toString(),
                        "userName",
                        user.getUsername())))
        .advisors(a -> a.params(Map.of(ChatMemory.CONVERSATION_ID, user.getId().toString())))
        .tools(tools)
        .toolContext(Map.of("documentId", documentId, "userId", user.getId()))
        .call()
        .content();
  }

  @Override
  public String queryWithMedia(String text, List<Media> media, User user, UUID documentId) {
    return chatClient
        .prompt()
        .user(u -> u.text(text).media(media.toArray(new Media[0])))
        .system(
            s ->
                s.params(
                    Map.of(
                        "currentDateTime",
                        LocalDateTime.now().toString(),
                        "userName",
                        user.getUsername())))
        .advisors(a -> a.params(Map.of(ChatMemory.CONVERSATION_ID, user.getId().toString())))
        .tools(tools)
        .toolContext(Map.of("documentId", documentId, "userId", user.getId()))
        .call()
        .content();
  }

  @Override
  @Transactional
  public SubmitResponse saveExpenseDocument(ExpenseDocument document) {
    validateDocument(document);
    document.setProcessingStatus(DocumentProcessingStatus.PROCESSING);

    expenseDocumentRepository.save(document);

    eventPublisher.publishEvent(
        new DocumentSavedEvent(document.getId(), document.getCreator().getId()));

    return new SubmitResponse(document.getId(), DocumentProcessingStatus.PROCESSING);
  }

  private void validateDocument(ExpenseDocument document) {
    boolean hasText = document.getRawText() != null && !document.getRawText().isBlank();
    boolean hasFile = document.getFileUrl() != null && !document.getFileUrl().isBlank();
    if (!hasText && !hasFile) {
      throw new BadRequestException("Either rawText or fileUrl must be provided");
    }
  }

  @Override
  public StatusResponse getDocumentStatus(UUID documentId, User user) {

    DocumentExpenseProjection document =
        expenseDocumentRepository
            .findByStatus(documentId, user)
            .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

    ExpenseData expenseData =
        document.expense() != null ? ExpenseData.from(document.expense()) : null;

    return new StatusResponse(
        document.documentId(), document.status(), document.aiResponse(), expenseData);
  }
}
