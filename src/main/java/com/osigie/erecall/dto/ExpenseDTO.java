package com.osigie.erecall.dto;

import com.osigie.erecall.domain.DocumentProcessingStatus;
import com.osigie.erecall.domain.DocumentType;
import com.osigie.erecall.domain.entity.ExpenseDocument;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class ExpenseDTO {

    @Data
    public static class Request {
        private final String rawText;
        private DocumentType type;
        private String fileUrl;
    }

    public record SubmitResponse(UUID documentId, DocumentProcessingStatus status, String responseText) {}

    public record StatusResponse(UUID documentId, DocumentProcessingStatus status, ExpenseData expense) {}

    public record ExpenseData(UUID id, BigDecimal amount, String merchant, String category, String description, OffsetDateTime expenseDate) {
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

    public static ExpenseDocument fromRequest(Request request) {
        return ExpenseDocument.builder()
                .rawText(request.rawText)
                .type(request.type)
                .fileUrl(request.fileUrl)
                .build();
    }
}
