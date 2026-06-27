package com.osigie.erecall.repo;

import com.osigie.erecall.domain.entity.ExpenseDocument;
import com.osigie.erecall.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExpenseDocumentRepository extends JpaRepository<ExpenseDocument, UUID> {
    Optional<ExpenseDocument> findByIdAndCreator(UUID id, User creator);
}
