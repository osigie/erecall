package com.osigie.erecall.service;

import com.osigie.erecall.domain.ExpenseCategory;
import com.osigie.erecall.domain.entity.Expense;
import com.osigie.erecall.domain.entity.ExpenseDocument;
import com.osigie.erecall.domain.entity.User;
import com.osigie.erecall.repo.ExpenseDocumentRepository;
import com.osigie.erecall.repo.ExpenseRepository;
import com.osigie.erecall.repo.spec.ExpenseSpecification;
import com.osigie.erecall.service.impl.ExpenseToolServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseToolServiceTests {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private ExpenseDocumentRepository expenseDocumentRepository;

    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private ExpenseToolServiceImpl toolService;

    private final UUID documentId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    private ToolContext toolContext() {
        var ctx = mock(ToolContext.class);
        when(ctx.getContext()).thenReturn(Map.of("documentId", documentId, "userId", userId));
        return ctx;
    }

    private ExpenseDocument expenseDocument() {
        var doc = ExpenseDocument.builder().rawText("test").build();
        ReflectionTestUtils.setField(doc, "id", documentId);
        return doc;
    }

    @Test
    void saveExpense_withMerchant_shouldSaveAndReturnFormattedString() {
        var doc = expenseDocument();
        when(expenseDocumentRepository.findById(documentId)).thenReturn(Optional.of(doc));
        when(expenseRepository.save(any())).thenAnswer(invocation -> {
            Expense saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
            return saved;
        });

        String result = toolService.saveExpense("Netflix", new BigDecimal("15.99"),
                "Netflix subscription", ExpenseCategory.ENTERTAINMENT,
                LocalDateTime.of(2024, 6, 1, 0, 0), toolContext());

        assertThat(result).isEqualTo("Saved Netflix subscription at Netflix for 15.99 under ENTERTAINMENT");

        var expenseCaptor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(expenseCaptor.capture());
        var saved = expenseCaptor.getValue();
        assertThat(saved.getMerchant()).isEqualTo("Netflix");
        assertThat(saved.getAmount()).isEqualByComparingTo(new BigDecimal("15.99"));
        assertThat(saved.getCategory()).isEqualTo(ExpenseCategory.ENTERTAINMENT);
        assertThat(saved.getDescription()).isEqualTo("Netflix subscription");
        assertThat(saved.getExpenseDocument()).isEqualTo(doc);

        verify(vectorStore).add(any());
    }

    @Test
    void saveExpense_withoutMerchant_shouldReturnFormattedStringWithoutMerchant() {
        var doc = expenseDocument();
        when(expenseDocumentRepository.findById(documentId)).thenReturn(Optional.of(doc));
        when(expenseRepository.save(any())).thenAnswer(invocation -> {
            Expense saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
            return saved;
        });

        String result = toolService.saveExpense(null, new BigDecimal("50.00"),
                "Groceries", ExpenseCategory.GROCERIES, null, toolContext());

        assertThat(result).isEqualTo("Saved Groceries for 50.00 under GROCERIES");

        var expenseCaptor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(expenseCaptor.capture());
        assertThat(expenseCaptor.getValue().getMerchant()).isNull();
    }

    @Test
    void getExpensesByDateRange_shouldReturnFormattedExpenses() {
        var start = LocalDateTime.of(2024, 1, 1, 0, 0);
        var end = LocalDateTime.of(2024, 1, 31, 23, 59, 59);

        var expense1 = Expense.builder()
                .expenseDate(LocalDateTime.of(2024, 1, 15, 12, 0))
                .merchant("Shell")
                .category(ExpenseCategory.FUEL)
                .amount(new BigDecimal("80.00"))
                .description("gas")
                .build();

        when(expenseRepository.findByExpenseDocumentCreatorIdAndExpenseDateBetween(userId, start, end))
                .thenReturn(List.of(expense1));

        var result = toolService.getExpensesByDateRange(start, end, toolContext());

        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).contains("Shell", "FUEL", "80.00", "gas");
    }

    @Test
    void searchDatabase_shouldFilterByUserId() {
        var expense = Expense.builder()
                .expenseDate(LocalDateTime.now())
                .merchant("Amazon")
                .category(ExpenseCategory.MISCELLANEOUS)
                .amount(new BigDecimal("29.99"))
                .build();

        when(expenseRepository.findAll(any(Specification.class))).thenReturn(List.of(expense));

        var result = toolService.searchDatabase(
                ExpenseCategory.MISCELLANEOUS, "Amazon", null, toolContext());

        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).contains("Amazon", "MISCELLANEOUS");
    }

    @Test
    void searchVector_shouldCallSimilaritySearch() {
        var aiDoc = new Document("bought groceries at Walmart");
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(aiDoc));

        var result = toolService.searchVector("grocery store items", toolContext());

        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).isEqualTo("bought groceries at Walmart");
    }
}
