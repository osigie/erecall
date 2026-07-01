package com.osigie.erecall.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.osigie.erecall.domain.DocumentProcessingStatus;
import com.osigie.erecall.domain.entity.ExpenseDocument;
import com.osigie.erecall.domain.entity.User;
import com.osigie.erecall.event.DocumentSavedEvent;
import com.osigie.erecall.repo.ExpenseDocumentRepository;
import com.osigie.erecall.service.impl.DocumentProcessingService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DocumentProcessingServiceTests {

  @Mock private ExpenseDocumentRepository expenseDocumentRepository;

  @Mock private ExpenseService expenseService;

  @Mock private FileStorageService fileStorageService;

  @InjectMocks private DocumentProcessingService processingService;

  private User createUser() {
    var user =
        User.builder()
            .email("user@test.com")
            .passwordHash("p")
            .username("u")
            .role(User.Role.USER)
            .build();
    ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
    return user;
  }

  @Test
  void handleDocumentSaved_withTextDocument_shouldProcessAndUpdateStatus() {
    var user = createUser();
    var docId = UUID.randomUUID();
    var document = ExpenseDocument.builder().rawText("lunch 25").creator(user).build();
    ReflectionTestUtils.setField(document, "id", docId);
    document.setProcessingStatus(DocumentProcessingStatus.PROCESSING);

    when(expenseDocumentRepository.findByIdWithCreator(docId)).thenReturn(Optional.of(document));
    when(expenseService.query("lunch 25", user, docId)).thenReturn("Saved lunch");

    processingService.handleDocumentSaved(new DocumentSavedEvent(docId, user.getId()));

    assertThat(document.getProcessingStatus()).isEqualTo(DocumentProcessingStatus.PROCESSED);
    assertThat(document.getAiResponse()).isEqualTo("Saved lunch");
    verify(expenseDocumentRepository).save(document);
    verify(expenseService, never()).queryWithMedia(any(), any(), any(), any());
  }

  @Test
  void handleDocumentSaved_withFileDocument_shouldGenerateUrlAndProcess() {
    var user = createUser();
    var docId = UUID.randomUUID();
    var document =
        ExpenseDocument.builder()
            .rawText("receipt")
            .fileUrl("uploads/receipt.pdf")
            .creator(user)
            .build();
    ReflectionTestUtils.setField(document, "id", docId);
    document.setProcessingStatus(DocumentProcessingStatus.PROCESSING);

    when(expenseDocumentRepository.findByIdWithCreator(docId)).thenReturn(Optional.of(document));
    when(fileStorageService.generatePresignedUrl(
            "uploads/receipt.pdf", java.time.Duration.ofMinutes(5)))
        .thenReturn("https://presigned.url/doc");
    when(expenseService.queryWithMedia(anyString(), anyList(), eq(user), eq(docId)))
        .thenReturn("Saved receipt");

    processingService.handleDocumentSaved(new DocumentSavedEvent(docId, user.getId()));

    assertThat(document.getProcessingStatus()).isEqualTo(DocumentProcessingStatus.PROCESSED);
    assertThat(document.getAiResponse()).isEqualTo("Saved receipt");
    verify(expenseService).queryWithMedia(anyString(), anyList(), eq(user), eq(docId));
  }

  @Test
  void handleDocumentSaved_withFileOnlyNoText_shouldUseDefaultPrompt() {
    var user = createUser();
    var docId = UUID.randomUUID();
    var document = ExpenseDocument.builder().fileUrl("uploads/receipt.pdf").creator(user).build();
    ReflectionTestUtils.setField(document, "id", docId);
    document.setProcessingStatus(DocumentProcessingStatus.PROCESSING);

    when(expenseDocumentRepository.findByIdWithCreator(docId)).thenReturn(Optional.of(document));
    when(fileStorageService.generatePresignedUrl(anyString(), any()))
        .thenReturn("https://presigned.url/doc");
    when(expenseService.queryWithMedia(anyString(), anyList(), eq(user), eq(docId)))
        .thenReturn("extracted");

    processingService.handleDocumentSaved(new DocumentSavedEvent(docId, user.getId()));

    assertThat(document.getProcessingStatus()).isEqualTo(DocumentProcessingStatus.PROCESSED);
    verify(expenseService)
        .queryWithMedia(
            eq("Extract expense information from this document."), anyList(), eq(user), eq(docId));
  }

  @Test
  void handleDocumentSaved_withException_shouldUpdateToFailed() {
    var user = createUser();
    var docId = UUID.randomUUID();
    var document = ExpenseDocument.builder().rawText("lunch 25").creator(user).build();
    ReflectionTestUtils.setField(document, "id", docId);
    document.setProcessingStatus(DocumentProcessingStatus.PROCESSING);

    when(expenseDocumentRepository.findByIdWithCreator(docId)).thenReturn(Optional.of(document));
    when(expenseService.query(anyString(), any(), any()))
        .thenThrow(new RuntimeException("LLM error"));

    processingService.handleDocumentSaved(new DocumentSavedEvent(docId, user.getId()));

    assertThat(document.getProcessingStatus()).isEqualTo(DocumentProcessingStatus.FAILED);
    assertThat(document.getAiResponse()).isEqualTo("Failed to process document");
  }

  @Test
  void handleDocumentSaved_withUnknownDocument_shouldDoNothing() {
    var docId = UUID.randomUUID();
    when(expenseDocumentRepository.findByIdWithCreator(docId)).thenReturn(Optional.empty());

    processingService.handleDocumentSaved(new DocumentSavedEvent(docId, UUID.randomUUID()));

    verify(expenseService, never()).query(anyString(), any(), any());
    verify(expenseDocumentRepository, never()).save(any());
  }
}
