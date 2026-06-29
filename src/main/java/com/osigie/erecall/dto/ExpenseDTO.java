package com.osigie.erecall.dto;

import com.osigie.erecall.domain.DocumentProcessingStatus;
import com.osigie.erecall.domain.entity.ExpenseDocument;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class ExpenseDTO {

    public record Request(
            String rawText,
            String fileUrl
    ) {
        public ExpenseDocument toDocument() {
            return ExpenseDocument.builder()
                    .rawText(rawText)
                    .fileUrl(fileUrl)
                    .build();
        }
    }

    public record SubmitResponse(UUID documentId, DocumentProcessingStatus status, String responseText) {}

    public record StatusResponse(UUID documentId, DocumentProcessingStatus status, String aiResponse, ExpenseData expense) {}

    public record ExpenseData(UUID id, BigDecimal amount, String merchant, String category, String description, LocalDateTime expenseDate) {
        public static ExpenseData from(com.osigie.erecall.domain.entity.Expense expense) {
            return new ExpenseData(
                    expense.getId(),
                    expense.getAmount(),
                    expense.getMerchant(),
                    expense.getCategory().name(),
                    expense.getDescription(),
                    expense.getExpenseDate()
            );
        }
    }
}
