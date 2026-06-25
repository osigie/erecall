package com.osigie.erecall.domain.valueObjects;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseExtraction(BigDecimal amount, String merchant, String category, String description,
                                LocalDate date) {
}
