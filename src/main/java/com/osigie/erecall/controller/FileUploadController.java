package com.osigie.erecall.controller;

import com.osigie.erecall.dto.BaseResponse;
import com.osigie.erecall.security.AuthHelper;
import com.osigie.erecall.service.FileStorageService;
import com.osigie.erecall.validation.FileValidator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/expenses")
public class FileUploadController {

    private final FileStorageService fileStorageService;
    private final AuthHelper authHelper;
    private final FileValidator fileValidator;

    public FileUploadController(FileStorageService fileStorageService, AuthHelper authHelper, FileValidator fileValidator) {
        this.fileStorageService = fileStorageService;
        this.authHelper = authHelper;
        this.fileValidator = fileValidator;
    }

    @PostMapping("/upload")
    public ResponseEntity<BaseResponse<String>> upload(@RequestParam("file") MultipartFile file) {
        authHelper.getAuthenticatedUser();
        fileValidator.validate(file);
        String key = fileStorageService.upload(file);
        return ResponseEntity.ok(BaseResponse.success(key));
    }
}
