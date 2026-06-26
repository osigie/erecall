package com.osigie.erecall.service.impl;

import com.osigie.erecall.domain.ExpenseCategory;
import com.osigie.erecall.domain.entity.Expense;
import com.osigie.erecall.domain.entity.ExpenseDocument;
import com.osigie.erecall.repo.ExpenseDocumentRepository;
import com.osigie.erecall.repo.ExpenseRepository;
import com.osigie.erecall.service.ExpenseTools;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Service
public class ExpenseToolsImpl implements ExpenseTools {
    private final ExpenseRepository expenseRepository;
    private final ExpenseDocumentRepository expenseDocumentRepository;
    private final VectorStore vectorStore;


    public ExpenseToolsImpl(ExpenseRepository expenseRepository, ExpenseDocumentRepository expenseDocumentRepository, VectorStore vectorStore) {
        this.expenseRepository = expenseRepository;
        this.expenseDocumentRepository = expenseDocumentRepository;
        this.vectorStore = vectorStore;
    }

    @Override
    @Tool(description = "Save a new expense. Use when the user is recording, adding, or logging an expense.")
    public String saveExpense(
            @ToolParam(required = false, description = "Merchant or vendor name, e.g. Netflix, Shell, Walmart") String merchant,
            @ToolParam(description = "Amount paid, e.g. 500, 29.99") BigDecimal amount,
            @ToolParam(description = "What the expense was for, e.g. 'Netflix monthly subscription'") String description,
            @ToolParam(required = false, description = "Date the expense occurred (ISO-8601). Defaults to today if not provided.") OffsetDateTime expenseDate,
            @ToolParam(description = "Category of the expense, e.g. ENTERTAINMENT, GROCERIES") ExpenseCategory category,
            ToolContext toolContext) {
        UUID documentId = (UUID) toolContext.getContext().get("documentId");
        ExpenseDocument document = expenseDocumentRepository.findById(documentId).orElseThrow();

        Expense expense = Expense.builder()
                .category(category)
                .amount(amount)
                .merchant(merchant)
                .description(description)
                .expenseDate(expenseDate != null ? expenseDate : OffsetDateTime.now())
                .expenseDocument(document)
                .build();

        expenseRepository.save(expense);

        vectorStore.add(List.of(
                new Document(description,
                        Map.of("expenseId", expense.getId(),
                                "merchant", expense.getMerchant(),
                                "category", expense.getCategory(),
                                "amount", expense.getAmount(),
                                "description", expense.getDescription(),
                                "date", expense.getExpenseDate()
                        ))));

        if (merchant != null) {
            return "Saved %s at %s for %s under %s".formatted(description, merchant, amount, category);
        }
        return "Saved %s for %s under %s".formatted(description, amount, category);
    }

    @Tool(description = """
            Retrieve expenses within a date range for reasoning and analysis.
            Use when the user specifies a time period like 'this month', 'last week', 'in June', or any date range.
            """)
    public List<String> getExpensesByDateRange(
            @ToolParam(description = "Start date of the range (inclusive)") OffsetDateTime startDate,
            @ToolParam(description = "End date of the range (inclusive)") OffsetDateTime endDate) {
        return expenseRepository.findByExpenseDateBetween(startDate, endDate)
                .stream()
                .map(e -> "%s | %s | %s | %s | %s".formatted(
                        e.getExpenseDate(), e.getMerchant(), e.getCategory(),
                        e.getAmount(), e.getDescription()))
                .toList();
    }

    @Override
    @Tool(description = "Search expenses. All parameters are optional and will be combined.")
    public List<String> searchDatabase(
            @ToolParam(description = "Expense category filter (e.g. GROCERIES, DINING_OUT, ENTERTAINMENT)") ExpenseCategory category,
            @ToolParam(description = "Merchant name to search for (partial match)") String merchant,
            @ToolParam(description = "Exact expense amount to match") BigDecimal amount) {
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

    @Tool(description = "Semantic search over expense descriptions for natural language queries")
    public List<String> searchVector(@ToolParam(description = "Natural language description to search for, e.g. 'things I bought at the grocery store'") String query) {
        return vectorStore.similaritySearch(SearchRequest.builder().query(query).topK(5).build()).stream().map(Document::getText).toList();
    }

}
