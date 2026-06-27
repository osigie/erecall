package com.osigie.erecall.repo.spec;

import com.osigie.erecall.domain.ExpenseCategory;
import com.osigie.erecall.domain.entity.Expense;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.UUID;

public class ExpenseSpecification {

    public static Specification<Expense> belongsToUser(UUID userId) {
        return (root, query, cb) -> cb.equal(
                root.get("expenseDocument").get("creator").get("id"), userId
        );
    }

    public static Specification<Expense> hasCategory(ExpenseCategory category) {
        return (root, query, cb) ->
                category == null
                        ? null
                        : cb.equal(root.get("category"), category);
    }

    public static Specification<Expense> hasMerchant(String merchant) {
        return (root, query, cb) ->
                merchant == null || merchant.isBlank()
                        ? null
                        : cb.like(cb.lower(root.get("merchant")), "%" + merchant.toLowerCase() + "%");
    }


    public static Specification<Expense> hasAmount(BigDecimal amount) {
        return (root, query, cb) ->
                amount == null
                        ? null
                        : cb.equal(root.get("amount"), amount);
    }

}
