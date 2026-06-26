package com.osigie.erecall.service.impl;

import com.osigie.erecall.domain.ExpenseCategory;
import com.osigie.erecall.domain.entity.Expense;
import com.osigie.erecall.domain.entity.ExpenseDocument;
import com.osigie.erecall.repo.ExpenseDocumentRepository;
import com.osigie.erecall.repo.ExpenseRepository;
import com.osigie.erecall.service.ExpenseTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Service
public class ExpenseToolsImpl implements ExpenseTools {
    private static final Logger log = LoggerFactory.getLogger(ExpenseToolsImpl.class);
    private final ExpenseRepository expenseRepository;
    private final ExpenseDocumentRepository expenseDocumentRepository;
    private final VectorStore vectorStore;


    public ExpenseToolsImpl(ExpenseRepository expenseRepository, ExpenseDocumentRepository expenseDocumentRepository, VectorStore vectorStore) {
        this.expenseRepository = expenseRepository;
        this.expenseDocumentRepository = expenseDocumentRepository;
        this.vectorStore = vectorStore;
    }

    private static String logCtx(ToolContext ctx) {
        return "documentId=%s, userId=%s".formatted(
                ctx.getContext().get("documentId"), ctx.getContext().get("userId"));
    }

    @Override
    @Tool(description = "Save a new expense. Use when the user is recording, adding, or logging an expense.")
    public String saveExpense(
            @ToolParam(required = false, description = "Merchant or vendor name, e.g. Netflix, Shell, Walmart") String merchant,
            @ToolParam(description = "Amount paid, e.g. 500, 29.99") BigDecimal amount,
            @ToolParam(description = "What the expense was for, e.g. 'Netflix monthly subscription'") String description,
            @ToolParam(description = "Category of the expense, e.g. ENTERTAINMENT, GROCERIES") ExpenseCategory category,
            ToolContext toolContext) {

        UUID documentId = (UUID) toolContext.getContext().get("documentId");

        log.info("saveExpense called: {}, amount={}, description={}, category={}, merchant={}",
                logCtx(toolContext), amount, description, category, merchant);


        ExpenseDocument document = expenseDocumentRepository.findById(documentId).orElseThrow();

        Expense expense = Expense.builder()
                .category(category)
                .amount(amount)
                .merchant(merchant)
                .description(description)
                .expenseDate(LocalDateTime.now())
                .expenseDocument(document)
                .build();

        expenseRepository.save(expense);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("expenseId", expense.getId().toString());
        metadata.put("category", expense.getCategory().name());
        metadata.put("amount", expense.getAmount().doubleValue());
        metadata.put("description", expense.getDescription());
        if (expense.getMerchant() != null) {
            metadata.put("merchant", expense.getMerchant());
        }

        vectorStore.add(List.of(new Document(description, metadata)));

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
            @ToolParam(description = "Start of range (inclusive) — use start of day, e.g. 2024-01-15T00:00:00 for January 15") LocalDateTime startDate,
            @ToolParam(description = "End of range (inclusive) — use end of day, e.g. 2024-01-15T23:59:59 for January 15") LocalDateTime endDate,
            ToolContext toolContext) {
        log.info("getExpensesByDateRange called: {}, start={}, end={}",
                logCtx(toolContext), startDate, endDate);
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
            @ToolParam(description = "Exact expense amount to match") BigDecimal amount,
            ToolContext toolContext) {
        log.info("searchDatabase called: {}, category={}, merchant={}, amount={}",
                logCtx(toolContext), category, merchant, amount);
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
    public List<String> searchVector(@ToolParam(description = "Natural language description to search for, e.g. 'things I bought at the grocery store'") String query, ToolContext toolContext) {
        log.info("searchVector called: {}, query={}",
                logCtx(toolContext), query);
        return vectorStore.similaritySearch(SearchRequest.builder().query(query).topK(5).build()).stream().map(Document::getText).toList();
    }

}
