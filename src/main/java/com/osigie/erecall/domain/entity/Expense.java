package com.osigie.erecall.domain.entity;

import com.osigie.erecall.domain.ExpenseCategory;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "expenses")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Expense {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "expense_document_id", nullable = false, updatable = false)
  private ExpenseDocument expenseDocument;

  @Column(name = "amount", nullable = false)
  private BigDecimal amount;

  @Column(name = "description")
  private String description;

  @Column(name = "category", nullable = false, length = 50)
  @Enumerated(EnumType.STRING)
  private ExpenseCategory category;

  @Column(name = "merchant", length = 100)
  private String merchant;

  @Column(name = "expense_date", nullable = false)
  private LocalDateTime expenseDate;

  @Builder
  public Expense(
      ExpenseCategory category,
      String description,
      BigDecimal amount,
      String merchant,
      LocalDateTime expenseDate,
      ExpenseDocument expenseDocument) {
    this.category = category;
    this.description = description;
    this.amount = amount;
    this.merchant = merchant;
    this.expenseDate = expenseDate;
    this.expenseDocument = expenseDocument;
  }

  @PrePersist
  public void prePersist() {
    this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
  }
}
