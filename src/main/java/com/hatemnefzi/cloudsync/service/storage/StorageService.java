package com.hatemnefzi.cloudsync.service.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

public interface StorageService {
    
    /**
     * Store a file and return the storage key
     */
    String store(MultipartFile file, Long userId, String filename) throws IOException;
    
    /**
     * Get file as byte array
     */
    byte[] getFile(String storageKey) throws IOException;
    
    /**
     * Get file as InputStream
     */
    InputStream getFileStream(String storageKey) throws IOException;
    
    /**
     * Delete a file
     */
    void delete(String storageKey) throws IOException;
    
    /**
     * Check if file exists
     */
    boolean exists(String storageKey);
    
    /**
     * Get file size
     */
    long getFileSize(String storageKey) throws IOException;
}