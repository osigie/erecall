package com.osigie.erecall.repo;

import com.osigie.erecall.domain.entity.ExpenseDocument;
import com.osigie.erecall.domain.entity.User;
import com.osigie.erecall.repo.projection.DocumentExpenseProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExpenseDocumentRepository extends JpaRepository<ExpenseDocument, UUID> {
    Optional<ExpenseDocument> findByIdAndCreator(UUID id, User creator);

    @Query("""
                SELECT ex
                FROM ExpenseDocument ex
                JOIN FETCH ex.creator
                WHERE ex.id = :id
            """)
    Optional<ExpenseDocument> findByIdWithCreator(UUID id);

    @Query("""
            SELECT d.id, d.processingStatus, d.aiResponse, e
            FROM ExpenseDocument d
            LEFT JOIN Expense e
            ON e.expenseDocument.id = d.id
            WHERE d.id = :documentId
            AND d.creator = :user
            """)
    Optional<DocumentExpenseProjection> findByStatus(UUID documentId, User user);
}
