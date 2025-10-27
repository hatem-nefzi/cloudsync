package com.hatemnefzi.cloudsync.service.storage;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3StorageService implements StorageService {

    private final AmazonS3 s3Client;

    @Value("${storage.s3.bucket-name}")
    private String bucketName;

    @Override
    public String store(MultipartFile file, Long userId, String filename) throws IOException {
        String key = generateKey(userId, filename);
        
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());
        
        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest request = new PutObjectRequest(bucketName, key, inputStream, metadata);
            s3Client.putObject(request);
            log.info("Stored file in S3: bucket={}, key={}", bucketName, key);
            return key;
        } catch (Exception e) {
            log.error("Failed to store file in S3: {}", e.getMessage());
            throw new IOException("Failed to store file in S3", e);
        }
    }

    @Override
    public byte[] getFile(String storageKey) throws IOException {
        try {
            S3Object s3Object = s3Client.getObject(bucketName, storageKey);
            try (InputStream inputStream = s3Object.getObjectContent()) {
                return inputStream.readAllBytes();
            }
        } catch (Exception e) {
            log.error("Failed to get file from S3: {}", e.getMessage());
            throw new IOException("Failed to get file from S3", e);
        }
    }

    @Override
    public InputStream getFileStream(String storageKey) throws IOException {
        try {
            S3Object s3Object = s3Client.getObject(bucketName, storageKey);
            return s3Object.getObjectContent();
        } catch (Exception e) {
            log.error("Failed to get file stream from S3: {}", e.getMessage());
            throw new IOException("Failed to get file stream from S3", e);
        }
    }

    @Override
    public void delete(String storageKey) throws IOException {
        try {
            s3Client.deleteObject(bucketName, storageKey);
            log.info("Deleted file from S3: {}", storageKey);
        } catch (Exception e) {
            log.error("Failed to delete file from S3: {}", e.getMessage());
            throw new IOException("Failed to delete file from S3", e);
        }
    }

    @Override
    public boolean exists(String storageKey) {
        try {
            s3Client.getObjectMetadata(bucketName, storageKey);
            return true;
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() == 404) {
                return false;
            }
            throw e;
        }
    }

    @Override
    public long getFileSize(String storageKey) throws IOException {
        try {
            ObjectMetadata metadata = s3Client.getObjectMetadata(bucketName, storageKey);
            return metadata.getContentLength();
        } catch (Exception e) {
            log.error("Failed to get file size from S3: {}", e.getMessage());
            throw new IOException("Failed to get file size from S3", e);
        }
    }

    private String generateKey(Long userId, String filename) {
        String uuid = UUID.randomUUID().toString();
        return String.format("%d/%s_%s", userId, uuid, filename);
    }
}