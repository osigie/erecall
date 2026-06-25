package com.osigie.erecall.repo;

import com.osigie.erecall.domain.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
    List<Expense> findByExpenseDateBetween(OffsetDateTime startDate, OffsetDateTime endDate);

    Optional<Expense> findByExpenseDocumentId(UUID expenseDocumentId);
}
