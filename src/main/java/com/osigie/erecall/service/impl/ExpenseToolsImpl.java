package com.osigie.erecall.service.impl;

import com.osigie.erecall.domain.ExpenseCategory;
import com.osigie.erecall.domain.entity.Expense;
import com.osigie.erecall.domain.valueObjects.ExpenseExtraction;
import com.osigie.erecall.repo.ExpenseRepository;
import com.osigie.erecall.service.ExpenseTools;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


@Service
public class ExpenseToolsImpl implements ExpenseTools {
    private final ExtractionServiceImpl extractionService;
    private final ExpenseRepository expenseRepository;


    public ExpenseToolsImpl(ExtractionServiceImpl extractionService, ExpenseRepository expenseRepository) {
        this.extractionService = extractionService;
        this.expenseRepository = expenseRepository;
    }

    @Override
    @Tool(description = "Save a new expense from raw text input. Use when the user is recording, adding, or logging an expense.")
    public String saveExpense(String rawText) {
        ExpenseExtraction expenseExtraction = extractionService.extract(rawText);

        Expense expense = Expense.builder()
//              TODO: think of how to allow llm use from my enum category
//              .category(expenseExtraction.category())
                .category(ExpenseCategory.MISCELLANEOUS)
                .amount(expenseExtraction.amount())
                .description(expenseExtraction.description())
                .expenseDate(expenseExtraction.date())
//              .expenseDocument()
                .build();
        expenseRepository.save(expense);

//        TODO: save to vector


        return "Saved Merchant: %s, Category: %s, Description: %s, Amount: %s".formatted(expenseExtraction.category(), expenseExtraction.category(), expenseExtraction.description(), expenseExtraction.amount());
    }

    @Tool(description = """
            Retrieve expenses within a date range for reasoning and analysis.
            Use when the user specifies a time period like 'this month', 'last week', 'in June', or any date range.
            """)
    public List<String> getExpensesByDateRange(LocalDate startDate, LocalDate endDate) {
        return expenseRepository.findByDateBetween(startDate, endDate)
                .stream()
                .map(e -> "%s | %s | %s | %s | %s".formatted(
                        e.getExpenseDate(), e.getMerchant(), e.getCategory(),
                        e.getAmount(), e.getDescription()))
                .toList();
    }

    @Override
    @Tool(description = "Search expenses. All parameters are optional and will be combined.")
    public List<String> searchDatabase(ExpenseCategory category, String merchant, BigDecimal amount) {
        Expense probe = Expense.builder()
                .merchant(merchant != null && !merchant.isBlank() ? merchant : null)
                .category(category)
                .amount(amount)
                .build();

        Example<Expense> example = Example.of(probe);

        return expenseRepository.findAll(example).stream()
                .map(e -> "%s | %s | %s | %s".formatted(
                        e.getExpenseDate(), e.getMerchant(), e.getCategory(), e.getAmount()))
                .toList();

    }

    @Override
    @Tool(description = "Semantic search over expense descriptions for natural language queries")
    public List<String> searchVector(String query) {
        return List.of();
    }

}
