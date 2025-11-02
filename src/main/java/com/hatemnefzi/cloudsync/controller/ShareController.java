package com.hatemnefzi.cloudsync.controller;

import com.hatemnefzi.cloudsync.dto.PublicFileDownload;
import com.hatemnefzi.cloudsync.dto.PublicFileResponse;
import com.hatemnefzi.cloudsync.dto.ShareCreateRequest;
import com.hatemnefzi.cloudsync.dto.ShareResponse;
import com.hatemnefzi.cloudsync.service.ShareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ShareController {

    private final ShareService shareService;

    // Create share (private or public)
    @PostMapping("/shares")
    public ResponseEntity<ShareResponse> createShare(
            @Valid @RequestBody ShareCreateRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        ShareResponse response = shareService.createShare(request, userId);
        return ResponseEntity.ok(response);
    }

    // Get shares I created
    @GetMapping("/shares/my")
    public ResponseEntity<List<ShareResponse>> getMyShares(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<ShareResponse> shares = shareService.getMyShares(userId);
        return ResponseEntity.ok(shares);
    }

    // Get shares where I'm the recipient
    @GetMapping("/shares/with-me")
    public ResponseEntity<List<ShareResponse>> getSharedWithMe(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<ShareResponse> shares = shareService.getSharedWithMe(userId);
        return ResponseEntity.ok(shares);
    }

    // Revoke share
    @DeleteMapping("/shares/{shareId}")
    public ResponseEntity<Void> revokeShare(
            @PathVariable Long shareId,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        shareService.revokeShare(shareId, userId);
        return ResponseEntity.noContent().build();
    }

    // Update share expiry
    @PatchMapping("/shares/{shareId}/expiry")
    public ResponseEntity<ShareResponse> updateShareExpiry(
            @PathVariable Long shareId,
            @RequestParam String expiresAt,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        LocalDateTime expiry = LocalDateTime.parse(expiresAt);
        ShareResponse response = shareService.updateShareExpiry(shareId, expiry, userId);
        return ResponseEntity.ok(response);
    }

    // ========== PUBLIC ENDPOINTS (no auth required) ==========

    // Get public file info
    @GetMapping("/share/{shareToken}")
    public ResponseEntity<?> getPublicFileInfo(@PathVariable String shareToken) {
        try {
            PublicFileResponse response = shareService.getPublicFileInfo(shareToken);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    // Download public file
    @GetMapping("/share/{shareToken}/download")
    public ResponseEntity<?> downloadPublicFile(@PathVariable String shareToken) {
        try {
            PublicFileDownload download = shareService.downloadPublicFile(shareToken);
            
            ByteArrayResource resource = new ByteArrayResource(download.getFileData());
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(download.getMimeType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                           "attachment; filename=\"" + download.getFileName() + "\"")
                    .body(resource);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to download file: " + e.getMessage()));
        }
    }

    // Simple error response class
    private record ErrorResponse(String error) {}
}