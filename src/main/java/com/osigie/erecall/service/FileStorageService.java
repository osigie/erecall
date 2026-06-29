package com.osigie.erecall.service;

import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

public interface FileStorageService {
    String upload(MultipartFile file);
    String generatePresignedUrl(String key, Duration expiration);
}
