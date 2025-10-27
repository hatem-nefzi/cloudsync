package com.hatemnefzi.cloudsync.config;

import com.hatemnefzi.cloudsync.service.storage.LocalStorageService;
import com.hatemnefzi.cloudsync.service.storage.S3StorageService;
import com.hatemnefzi.cloudsync.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@RequiredArgsConstructor
public class StorageConfig {

    private final S3StorageService s3StorageService;
    private final LocalStorageService localStorageService;

    @Value("${storage.type}")
    private String storageType;

    @Bean
    @Primary
    public StorageService storageService() {
        if ("s3".equalsIgnoreCase(storageType)) {
            return s3StorageService;
        } else {
            return localStorageService;
        }
    }
}