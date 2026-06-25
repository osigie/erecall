package com.osigie.erecall.domain.entity;

import com.osigie.erecall.domain.DocumentProcessingStatus;
import com.osigie.erecall.domain.DocumentType;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "expense_documents", indexes = {
        @Index(name = "idx_expense_document_user_id", columnList = "user_id"),
        @Index(name = "idx_expense_document_processing_status", columnList = "processing_status")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ExpenseDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private DocumentType type;

    @Column(name = "raw_text", columnDefinition = "TEXT")
    private String rawText;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 50)
    private DocumentProcessingStatus processingStatus;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User creator;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }


    @Builder
    public ExpenseDocument(String rawText, String fileUrl, DocumentType type, User creator) {
        this.rawText = rawText;
        this.fileUrl = fileUrl;
        this.processingStatus = DocumentProcessingStatus.PENDING;
        this.creator = creator;
        this.type = type;
    }

}
