package com.hatemnefzi.cloudsync.service;

import com.hatemnefzi.cloudsync.dto.PublicFileDownload;
import com.hatemnefzi.cloudsync.dto.PublicFileResponse;
import com.hatemnefzi.cloudsync.dto.ShareCreateRequest;
import com.hatemnefzi.cloudsync.dto.ShareResponse;
import com.hatemnefzi.cloudsync.entity.*;
import com.hatemnefzi.cloudsync.repository.*;
import com.hatemnefzi.cloudsync.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShareService {

    private final ShareRepository shareRepository;
    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final StorageService storageService; // ← Add this

    @Value("${app.base-url:http://localhost:8082}")
    private String baseUrl;

    @Transactional
    public ShareResponse createShare(ShareCreateRequest request, Long userId) {
        User sharedBy = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate: must have either fileId or folderId, not both
        if ((request.getFileId() == null && request.getFolderId() == null) ||
            (request.getFileId() != null && request.getFolderId() != null)) {
            throw new RuntimeException("Must specify either fileId or folderId, not both");
        }

        File file = null;
        Folder folder = null;

        // Get file if sharing file
        if (request.getFileId() != null) {
            file = fileRepository.findByIdAndDeletedAtIsNull(request.getFileId())
                    .orElseThrow(() -> new RuntimeException("File not found"));
            
            // Check ownership
            if (!file.getOwner().getId().equals(userId)) {
                throw new RuntimeException("Cannot share file you don't own");
            }
        }

        // Get folder if sharing folder
        if (request.getFolderId() != null) {
            folder = folderRepository.findByIdAndOwner(request.getFolderId(), sharedBy)
                    .orElseThrow(() -> new RuntimeException("Folder not found"));
        }

        // Get user to share with (if not public)
        User sharedWith = null;
        String shareToken = null;

        // PUBLIC SHARE: Generate token if isPublic=true OR no email provided
        if (request.isPublic() || (request.getSharedWithEmail() == null || request.getSharedWithEmail().isEmpty())) {
            shareToken = UUID.randomUUID().toString();
            log.info("Generated public share token: {}", shareToken);
        } else {
            // PRIVATE SHARE: Find recipient user
            sharedWith = userRepository.findByEmail(request.getSharedWithEmail())
                    .orElseThrow(() -> new RuntimeException("User with email " + request.getSharedWithEmail() + " not found"));
            log.info("Creating private share with user: {}", request.getSharedWithEmail());
        }

        // Default permission to VIEW if not specified
        SharePermission permission = request.getPermission() != null ? request.getPermission() : SharePermission.VIEW;

        // Create share
        Share share = Share.builder()
                .file(file)
                .folder(folder)
                .sharedBy(sharedBy)
                .sharedWith(sharedWith)
                .permission(permission)
                .shareToken(shareToken)
                .expiresAt(request.getExpiresAt())
                .build();

        share = shareRepository.save(share);

        // Log activity
        logActivity(sharedBy, ActivityType.SHARE, 
                   file != null ? "FILE" : "FOLDER", 
                   file != null ? file.getId() : folder.getId());

        log.info("Share created: id={}, fileId={}, folderId={}, shareToken={}, public={}", 
                 share.getId(), 
                 file != null ? file.getId() : null, 
                 folder != null ? folder.getId() : null,
                 shareToken != null ? shareToken : "none",
                 shareToken != null);

        return mapToShareResponse(share);
    }

    @Transactional(readOnly = true)
    public List<ShareResponse> getMyShares(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Share> shares = shareRepository.findBySharedByOrderByCreatedAtDesc(user);

        return shares.stream()
                .map(this::mapToShareResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ShareResponse> getSharedWithMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Share> shares = shareRepository.findBySharedWithOrderByCreatedAtDesc(user);

        return shares.stream()
                .map(this::mapToShareResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PublicFileResponse getPublicFileInfo(String shareToken) {
        Share share = shareRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new RuntimeException("Share link not found or has been revoked"));

        // Check if expired
        if (share.getExpiresAt() != null && share.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Share link has expired");
        }

        // Only support file sharing for now (not folder)
        if (share.getFile() == null) {
            throw new RuntimeException("This share link is for a folder, not supported yet");
        }

        File file = share.getFile();

        return PublicFileResponse.builder()
                .fileName(file.getName())
                .size(file.getSize())
                .mimeType(file.getMimeType())
                .sharedByName(share.getSharedBy().getFullName())
                .sharedAt(share.getCreatedAt())
                .expiresAt(share.getExpiresAt())
                .canDownload(share.getPermission() == SharePermission.VIEW || 
                            share.getPermission() == SharePermission.EDIT)
                .build();
    }

    @Transactional(readOnly = true)
    public PublicFileDownload downloadPublicFile(String shareToken) throws IOException {
        Share share = shareRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new RuntimeException("Share link not found or has been revoked"));

        // Check if expired
        if (share.getExpiresAt() != null && share.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Share link has expired");
        }

        // Check permission
        if (share.getPermission() != SharePermission.VIEW && 
            share.getPermission() != SharePermission.EDIT) {
            throw new RuntimeException("No download permission");
        }

        if (share.getFile() == null) {
            throw new RuntimeException("Cannot download folder");
        }

        File file = share.getFile();

        // Get file data from storage
        byte[] fileData = storageService.getFile(file.getStorageKey());

        log.info("Public file downloaded: shareToken={}, fileId={}, fileName={}", 
                 shareToken, file.getId(), file.getName());

        return PublicFileDownload.builder()
                .fileData(fileData)
                .fileName(file.getName())
                .mimeType(file.getMimeType())
                .build();
    }

    @Transactional
    public void revokeShare(Long shareId, Long userId) {
        Share share = shareRepository.findById(shareId)
                .orElseThrow(() -> new RuntimeException("Share not found"));

        // Check ownership
        if (!share.getSharedBy().getId().equals(userId)) {
            throw new RuntimeException("Cannot revoke share you didn't create");
        }

        shareRepository.delete(share);

        log.info("Share revoked: id={}", shareId);
    }

    @Transactional
    public ShareResponse updateShareExpiry(Long shareId, LocalDateTime newExpiryDate, Long userId) {
        Share share = shareRepository.findById(shareId)
                .orElseThrow(() -> new RuntimeException("Share not found"));

        // Check ownership
        if (!share.getSharedBy().getId().equals(userId)) {
            throw new RuntimeException("Cannot update share you didn't create");
        }

        share.setExpiresAt(newExpiryDate);
        share = shareRepository.save(share);

        log.info("Share expiry updated: id={}, newExpiry={}", shareId, newExpiryDate);

        return mapToShareResponse(share);
    }

    private ShareResponse mapToShareResponse(Share share) {
        boolean isExpired = share.getExpiresAt() != null && 
                           share.getExpiresAt().isBefore(LocalDateTime.now());

        String shareUrl = null;
        if (share.getShareToken() != null) {
            shareUrl = baseUrl + "/api/share/" + share.getShareToken();
        }

        return ShareResponse.builder()
                .id(share.getId())
                .fileId(share.getFile() != null ? share.getFile().getId() : null)
                .fileName(share.getFile() != null ? share.getFile().getName() : null)
                .folderId(share.getFolder() != null ? share.getFolder().getId() : null)
                .folderName(share.getFolder() != null ? share.getFolder().getName() : null)
                .sharedByEmail(share.getSharedBy().getEmail())
                .sharedWithEmail(share.getSharedWith() != null ? share.getSharedWith().getEmail() : null)
                .permission(share.getPermission())
                .shareToken(share.getShareToken())
                .shareUrl(shareUrl)
                .expiresAt(share.getExpiresAt())
                .createdAt(share.getCreatedAt())
                .isExpired(isExpired)
                .build();
    }

    private void logActivity(User user, ActivityType action, String entityType, Long entityId) {
        Activity activity = Activity.builder()
                .user(user)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .build();
        activityRepository.save(activity);
    }
}