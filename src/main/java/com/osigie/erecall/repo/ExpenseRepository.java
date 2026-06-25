package com.osigie.erecall.repo;

import com.osigie.erecall.domain.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
    List<Expense> findByDateBetween(LocalDate startDate, LocalDate endDate);

    List<Expense> findByMerchant(String merchant);

    List<Expense> findByAmount(BigDecimal amount);

    List<Expense> findByCategory(String category);

    Optional<Expense> findByExpenseDocumentId(UUID expenseDocumentId);
}
