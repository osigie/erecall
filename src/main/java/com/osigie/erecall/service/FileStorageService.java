package com.osigie.erecall.service;

import java.time.Duration;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
  String upload(MultipartFile file);

  String generatePresignedUrl(String key, Duration expiration);
}
