package com.osigie.erecall.service;

import com.osigie.erecall.domain.ExpenseCategory;
import org.springframework.ai.chat.model.ToolContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ExpenseTools {

    String saveExpense(String rawText, ToolContext toolContext);

    List<String> getExpensesByDateRange(LocalDate startDate, LocalDate endDate);

    List<String> searchDatabase(ExpenseCategory category, String merchant, BigDecimal amount);

    List<String> searchVector(String query);
}
