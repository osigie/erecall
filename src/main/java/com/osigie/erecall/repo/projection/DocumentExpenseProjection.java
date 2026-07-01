package com.osigie.erecall.repo.projection;

import com.osigie.erecall.domain.DocumentProcessingStatus;
import com.osigie.erecall.domain.entity.Expense;
import java.util.UUID;

public record DocumentExpenseProjection(
    UUID documentId, DocumentProcessingStatus status, String aiResponse, Expense expense) {}
