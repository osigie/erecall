package com.osigie.erecall.service;

import com.osigie.erecall.domain.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ExpenseTools {

    String saveExpense(String rawText);

    List<String> getExpensesByDateRange(LocalDate startDate, LocalDate endDate);

    //TODO: see if i can use enum in category
    List<String> searchDatabase(ExpenseCategory category, String merchant, BigDecimal amount);

    List<String> searchVector(String query);
}
