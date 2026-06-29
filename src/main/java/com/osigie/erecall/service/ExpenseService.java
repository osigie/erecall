package com.osigie.erecall.service;

import com.osigie.erecall.domain.entity.ExpenseDocument;
import com.osigie.erecall.domain.entity.User;
import com.osigie.erecall.dto.ExpenseDTO;
import org.springframework.ai.content.Media;

import java.util.List;
import java.util.UUID;

public interface ExpenseService {
    String query(String text, UUID userId, UUID documentId);

    String queryWithMedia(String text, List<Media> media, UUID userId, UUID documentId);

    ExpenseDTO.SubmitResponse saveExpenseDocument(ExpenseDocument document);

    ExpenseDTO.StatusResponse getDocumentStatus(UUID documentId, User user);
}
