package com.hatemnefzi.cloudsync.service.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
public class LocalStorageService implements StorageService {

    @Value("${storage.local.upload-dir}")
    private String uploadDir;

    @Override
    public String store(MultipartFile file, Long userId, String filename) throws IOException {
        Path userDir = Paths.get(uploadDir, userId.toString());
        Files.createDirectories(userDir);

        String uniqueFilename = UUID.randomUUID().toString() + "_" + filename;
        Path filePath = userDir.resolve(uniqueFilename);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored file locally: {}", filePath);
            return userId + "/" + uniqueFilename; // Return relative path
        } catch (IOException e) {
            log.error("Failed to store file locally: {}", e.getMessage());
            throw new IOException("Failed to store file locally", e);
        }
    }

    @Override
    public byte[] getFile(String storageKey) throws IOException {
        Path filePath = Paths.get(uploadDir, storageKey);
        return Files.readAllBytes(filePath);
    }

    @Override
    public InputStream getFileStream(String storageKey) throws IOException {
        Path filePath = Paths.get(uploadDir, storageKey);
        return Files.newInputStream(filePath);
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Path filePath = Paths.get(uploadDir, storageKey);
        Files.deleteIfExists(filePath);
        log.info("Deleted file locally: {}", filePath);
    }

    @Override
    public boolean exists(String storageKey) {
        Path filePath = Paths.get(uploadDir, storageKey);
        return Files.exists(filePath);
    }

    @Override
    public long getFileSize(String storageKey) throws IOException {
        Path filePath = Paths.get(uploadDir, storageKey);
        return Files.size(filePath);
    }
}