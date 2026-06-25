package com.osigie.erecall.domain.valueObjects;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ExpenseExtraction(BigDecimal amount, String merchant, String category, String description,
                                OffsetDateTime date) {
}
