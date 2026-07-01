package com.osigie.erecall.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.osigie.erecall.domain.DocumentProcessingStatus;
import com.osigie.erecall.domain.ExpenseCategory;
import com.osigie.erecall.domain.entity.Expense;
import com.osigie.erecall.domain.entity.ExpenseDocument;
import com.osigie.erecall.domain.entity.User;
import com.osigie.erecall.event.DocumentSavedEvent;
import com.osigie.erecall.exception.BadRequestException;
import com.osigie.erecall.exception.ResourceNotFoundException;
import com.osigie.erecall.repo.ExpenseDocumentRepository;
import com.osigie.erecall.repo.projection.DocumentExpenseProjection;
import com.osigie.erecall.service.impl.ExpenseServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTests {

  @Mock private ExpenseDocumentRepository expenseDocumentRepository;

  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private ExpenseServiceImpl expenseService;

  private User createUser() {
    var user =
        User.builder()
            .email("user@test.com")
            .passwordHash("encoded")
            .username("testuser")
            .role(User.Role.USER)
            .build();
    ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
    return user;
  }

  @Test
  void saveExpenseDocument_withRawText_shouldSaveAndPublishEvent() {
    var user = createUser();
    var document = ExpenseDocument.builder().rawText("dinner 50").creator(user).build();

    when(expenseDocumentRepository.save(any()))
        .thenAnswer(
            invocation -> {
              ExpenseDocument saved = invocation.getArgument(0);
              ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
              return saved;
            });

    var response = expenseService.saveExpenseDocument(document);

    assertThat(response.documentId()).isNotNull();
    assertThat(response.status()).isEqualTo(DocumentProcessingStatus.PROCESSING);
    assertThat(document.getProcessingStatus()).isEqualTo(DocumentProcessingStatus.PROCESSING);

    var eventCaptor = ArgumentCaptor.forClass(DocumentSavedEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue().documentId()).isEqualTo(response.documentId());
    assertThat(eventCaptor.getValue().userId()).isEqualTo(user.getId());
  }

  @Test
  void saveExpenseDocument_withFileUrl_shouldSaveAndPublishEvent() {
    var user = createUser();
    var document = ExpenseDocument.builder().fileUrl("uploads/test.pdf").creator(user).build();

    when(expenseDocumentRepository.save(any()))
        .thenAnswer(
            invocation -> {
              ExpenseDocument saved = invocation.getArgument(0);
              ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
              return saved;
            });

    var response = expenseService.saveExpenseDocument(document);

    assertThat(response.status()).isEqualTo(DocumentProcessingStatus.PROCESSING);
    verify(eventPublisher).publishEvent(any(DocumentSavedEvent.class));
  }

  @Test
  void saveExpenseDocument_withEmptyBody_shouldThrowBadRequest() {
    var user = createUser();
    var document = ExpenseDocument.builder().creator(user).build();

    assertThatThrownBy(() -> expenseService.saveExpenseDocument(document))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Either rawText or fileUrl must be provided");

    verify(expenseDocumentRepository, never()).save(any());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void getDocumentStatus_withFoundDocument_shouldReturnStatusResponse() {
    var user = createUser();
    var documentId = UUID.randomUUID();
    var expense =
        Expense.builder()
            .category(ExpenseCategory.DINING_OUT)
            .amount(new BigDecimal("29.99"))
            .merchant("McDonald's")
            .description("lunch")
            .expenseDate(LocalDateTime.now())
            .build();
    ReflectionTestUtils.setField(expense, "id", UUID.randomUUID());

    var projection =
        new DocumentExpenseProjection(
            documentId, DocumentProcessingStatus.PROCESSED, "Saved", expense);

    when(expenseDocumentRepository.findByStatus(documentId, user))
        .thenReturn(Optional.of(projection));

    var response = expenseService.getDocumentStatus(documentId, user);

    assertThat(response.documentId()).isEqualTo(documentId);
    assertThat(response.status()).isEqualTo(DocumentProcessingStatus.PROCESSED);
    assertThat(response.aiResponse()).isEqualTo("Saved");
    assertThat(response.expense()).isNotNull();
    assertThat(response.expense().amount()).isEqualByComparingTo(new BigDecimal("29.99"));
    assertThat(response.expense().merchant()).isEqualTo("McDonald's");
  }

  @Test
  void getDocumentStatus_withNoExpense_shouldReturnNullExpense() {
    var user = createUser();
    var documentId = UUID.randomUUID();

    var projection =
        new DocumentExpenseProjection(documentId, DocumentProcessingStatus.PROCESSING, null, null);

    when(expenseDocumentRepository.findByStatus(documentId, user))
        .thenReturn(Optional.of(projection));

    var response = expenseService.getDocumentStatus(documentId, user);

    assertThat(response.documentId()).isEqualTo(documentId);
    assertThat(response.status()).isEqualTo(DocumentProcessingStatus.PROCESSING);
    assertThat(response.expense()).isNull();
  }

  @Test
  void getDocumentStatus_withNotFound_shouldThrowResourceNotFound() {
    var user = createUser();
    var documentId = UUID.randomUUID();

    when(expenseDocumentRepository.findByStatus(documentId, user)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> expenseService.getDocumentStatus(documentId, user))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Document not found");
  }
}
