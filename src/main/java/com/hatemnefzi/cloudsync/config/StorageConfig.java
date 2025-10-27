package com.hatemnefzi.cloudsync.config;

import com.amazonaws.services.s3.AmazonS3;
import com.hatemnefzi.cloudsync.service.storage.LocalStorageService;
import com.hatemnefzi.cloudsync.service.storage.S3StorageService;
import com.hatemnefzi.cloudsync.service.storage.StorageService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@Slf4j
public class StorageConfig {

    @Value("${storage.type}")
    private String storageType;

    @Value("${storage.s3.bucket-name}")
    private String bucketName;

    @Bean
    @Primary
    public StorageService storageService(AmazonS3 amazonS3) {
        if ("s3".equalsIgnoreCase(storageType)) {
            log.info("🔵 Using S3 Storage Service");
            return new S3StorageService(amazonS3, bucketName);
        } else {
            log.info("🟡 Using Local Storage Service");
            return new LocalStorageService();
        }
    }
}