package com.osigie.erecall.repo;

import com.osigie.erecall.domain.entity.Expense;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpenseRepository
    extends JpaRepository<Expense, UUID>, JpaSpecificationExecutor<Expense> {

  List<Expense> findByExpenseDocumentCreatorIdAndExpenseDateBetween(
      UUID userId, LocalDateTime startDate, LocalDateTime endDate);
}
