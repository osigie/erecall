package com.osigie.erecall.service.impl;

import com.osigie.erecall.config.S3Properties;
import com.osigie.erecall.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;

@Service
@Slf4j
public class S3FileStorageService implements FileStorageService {
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;

    public S3FileStorageService(S3Properties properties) {
        this.bucketName = properties.bucketName();

        var credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.accessKey(), properties.secretKey()));

        var s3Config = S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build();

        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(properties.endpoint()))
                .credentialsProvider(credentials)
                .region(Region.of(properties.region()))
                .forcePathStyle(true)
                .build();

        this.s3Presigner = S3Presigner.builder()
                .endpointOverride(URI.create(properties.endpoint()))
                .credentialsProvider(credentials)
                .region(Region.of(properties.region()))
                .serviceConfiguration(s3Config)
                .build();
    }

    @Override
    public String upload(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        String key = UUID.randomUUID() + extension;

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .acl(ObjectCannedACL.PRIVATE)
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));

            log.info("Uploaded file to S3: key={}", key);
            return key;
        } catch (Exception e) {
            log.error("Failed to upload file to S3: key={}", key, e);
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    @Override
    public String generatePresignedUrl(String key, Duration expiration) {
        try {
            var presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(expiration)
                    .getObjectRequest(r -> r.bucket(bucketName).key(key))
                    .build();

            String url = s3Presigner.presignGetObject(presignRequest).url().toString();
            log.info("Generated presigned URL for key={}, expires in {}s", key, expiration.toSeconds());
            return url;
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for key={}", key, e);
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }
}
