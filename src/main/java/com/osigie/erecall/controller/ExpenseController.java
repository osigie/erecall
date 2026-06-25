package com.osigie.erecall.controller;

import com.osigie.erecall.domain.entity.ExpenseDocument;
import com.osigie.erecall.dto.BaseResponse;
import com.osigie.erecall.dto.ExpenseDTO;
import com.osigie.erecall.security.AuthHelper;
import com.osigie.erecall.service.ExpenseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/api/v1/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final AuthHelper authHelper;

    public ExpenseController(ExpenseService expenseService, AuthHelper authHelper) {
        this.expenseService = expenseService;
        this.authHelper = authHelper;
    }

    @PostMapping
    public ResponseEntity<BaseResponse<ExpenseDTO.SubmitResponse>> submit(@RequestBody ExpenseDTO.Request request) {
        ExpenseDocument expenseDocument = ExpenseDTO.fromRequest(request);
        expenseDocument.setCreator(authHelper.getAuthenticatedUser());
        ExpenseDTO.SubmitResponse response = expenseService.saveExpenseDocument(expenseDocument);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<BaseResponse<ExpenseDTO.StatusResponse>> getStatus(@PathVariable UUID id) {
        ExpenseDTO.StatusResponse response = expenseService.getDocumentStatus(id, authHelper.getAuthenticatedUser());
        return ResponseEntity.ok(BaseResponse.success(response));
    }
}
