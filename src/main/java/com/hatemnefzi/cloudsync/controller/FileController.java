package com.hatemnefzi.cloudsync.controller;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.hatemnefzi.cloudsync.dto.FileInfoResponse;
import com.hatemnefzi.cloudsync.dto.FileUploadResponse;
import com.hatemnefzi.cloudsync.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;  // ← CORRECT IMPORT!
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final AmazonS3 s3Client;

    @Value("${storage.s3.bucket-name}")  // ← NOW THIS WILL WORK!
    private String bucketName;

    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folderId", required = false) Long folderId,
            Authentication authentication) throws IOException {
        
        Long userId = (Long) authentication.getPrincipal();
        FileUploadResponse response = fileService.uploadFile(file, userId, folderId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<FileInfoResponse>> getUserFiles(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<FileInfoResponse> files = fileService.getUserFiles(userId);
        return ResponseEntity.ok(files);
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long fileId,
            Authentication authentication) throws IOException {
        
        Long userId = (Long) authentication.getPrincipal();
        byte[] fileData = fileService.downloadFile(fileId, userId);
        
        ByteArrayResource resource = new ByteArrayResource(fileData);
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment")
                .body(resource);
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable Long fileId,
            Authentication authentication) throws IOException {
        
        Long userId = (Long) authentication.getPrincipal();
        fileService.deleteFile(fileId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/folder/{folderId}")
    public ResponseEntity<List<FileInfoResponse>> getFilesInFolder(
            @PathVariable Long folderId,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<FileInfoResponse> files = fileService.getFilesInFolder(folderId, userId);
        return ResponseEntity.ok(files);
    }

    @GetMapping("/s3-debug")
    public String s3Debug() {
        try {
            // List objects in bucket
            ObjectListing objectListing = s3Client.listObjects(bucketName);
            List<S3ObjectSummary> objects = objectListing.getObjectSummaries();

            StringBuilder result = new StringBuilder();
            result.append("=== S3 BUCKET DEBUG ===\n");
            result.append("Bucket: ").append(bucketName).append("\n");
            result.append("Total Objects: ").append(objects.size()).append("\n\n");
            
            if (objects.isEmpty()) {
                result.append("No objects found in bucket.\n");
                result.append("Make sure your S3 configuration is correct.\n");
            } else {
                result.append("Objects:\n");
                for (S3ObjectSummary object : objects) {
                    result.append("📄 ").append(object.getKey())
                          .append(" (").append(object.getSize()).append(" bytes)\n")
                          .append("    Last Modified: ").append(object.getLastModified()).append("\n");
                }
            }

            return result.toString();
        } catch (Exception e) {
            return "❌ S3 Debug Error: " + e.getMessage() + "\n" +
                   "Check your AWS credentials and bucket permissions.";
        }
    }
}