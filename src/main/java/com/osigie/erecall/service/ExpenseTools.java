package com.osigie.erecall.service;

import com.osigie.erecall.domain.ExpenseCategory;
import org.springframework.ai.chat.model.ToolContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface ExpenseTools {

    String saveExpense(String merchant, BigDecimal amount, String description, ExpenseCategory category, LocalDateTime expenseDate, ToolContext toolContext);

    List<String> getExpensesByDateRange(LocalDateTime startDate, LocalDateTime endDate, ToolContext toolContext);

    List<String> searchDatabase(ExpenseCategory category, String merchant, BigDecimal amount, ToolContext toolContext);

    List<String> searchVector(String query, ToolContext toolContext);
}
