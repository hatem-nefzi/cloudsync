package com.hatemnefzi.cloudsync.controller;

import com.hatemnefzi.cloudsync.dto.FolderCreateRequest;
import com.hatemnefzi.cloudsync.dto.FolderResponse;
import com.hatemnefzi.cloudsync.service.FolderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;

    @PostMapping
    public ResponseEntity<FolderResponse> createFolder(
            @Valid @RequestBody FolderCreateRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        FolderResponse response = folderService.createFolder(request, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/root")
    public ResponseEntity<List<FolderResponse>> getRootFolders(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<FolderResponse> folders = folderService.getRootFolders(userId);
        return ResponseEntity.ok(folders);
    }

    @GetMapping("/{folderId}/subfolders")
    public ResponseEntity<List<FolderResponse>> getSubfolders(
            @PathVariable Long folderId,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<FolderResponse> subfolders = folderService.getSubfolders(folderId, userId);
        return ResponseEntity.ok(subfolders);
    }

    @GetMapping("/tree")
    public ResponseEntity<FolderResponse> getFolderTree(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        FolderResponse tree = folderService.getFolderTree(userId);
        return ResponseEntity.ok(tree);
    }

    @DeleteMapping("/{folderId}")
    public ResponseEntity<Void> deleteFolder(
            @PathVariable Long folderId,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        folderService.deleteFolder(folderId, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{folderId}/rename")
    public ResponseEntity<FolderResponse> renameFolder(
            @PathVariable Long folderId,
            @RequestParam String newName,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        FolderResponse response = folderService.renameFolder(folderId, newName, userId);
        return ResponseEntity.ok(response);
    }
}