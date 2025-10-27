package com.hatemnefzi.cloudsync.controller;

import com.hatemnefzi.cloudsync.dto.FileInfoResponse;
import com.hatemnefzi.cloudsync.dto.FileUploadResponse;
import com.hatemnefzi.cloudsync.service.FileService;
import lombok.RequiredArgsConstructor;
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

    // adding a folder endpoint

        @GetMapping("/folder/{folderId}")
        public ResponseEntity<List<FileInfoResponse>> getFilesInFolder(
                @PathVariable Long folderId,
                Authentication authentication) {
            Long userId = (Long) authentication.getPrincipal();
            List<FileInfoResponse> files = fileService.getFilesInFolder(folderId, userId);
            return ResponseEntity.ok(files);
        }
}