package com.hatemnefzi.cloudsync.service;

import com.hatemnefzi.cloudsync.dto.FileInfoResponse;
import com.hatemnefzi.cloudsync.dto.FileUploadResponse;
import com.hatemnefzi.cloudsync.entity.Activity;
import com.hatemnefzi.cloudsync.entity.ActivityType;
import com.hatemnefzi.cloudsync.entity.File;
import com.hatemnefzi.cloudsync.entity.User;
import com.hatemnefzi.cloudsync.repository.ActivityRepository;
import com.hatemnefzi.cloudsync.repository.FileRepository;
import com.hatemnefzi.cloudsync.repository.UserRepository;
import com.hatemnefzi.cloudsync.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;
//adding imports for file service that rsupport folders
import com.hatemnefzi.cloudsync.entity.Folder;
import com.hatemnefzi.cloudsync.repository.FolderRepository;
//adding imports for file service that support file versionning
import com.hatemnefzi.cloudsync.entity.FileVersion;
import com.hatemnefzi.cloudsync.repository.FileVersionRepository;
import com.hatemnefzi.cloudsync.dto.FileVersionResponse;


@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final StorageService storageService;
    //adding imports for file service that support folders
    private final FolderRepository folderRepository;
    //adding imports for file service that support file versionning
    private final FileVersionRepository fileVersionRepository;

    @Transactional
    public FileUploadResponse uploadFile(MultipartFile multipartFile, Long userId, Long folderId) throws IOException {

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Get folder if specified
        Folder folder = null;
        if (folderId != null) {
            folder = folderRepository.findByIdAndOwner(folderId, user)
                .orElseThrow(() -> new RuntimeException("Folder not found"));
        }
        // Check storage quota
        if (user.getStorageUsed() + multipartFile.getSize() > user.getStorageLimit()) {
            throw new RuntimeException("Storage quota exceeded");
        }

        // Calculate file checksum (for deduplication)
        String checksum = calculateChecksum(multipartFile.getInputStream());

        // Check if file with same checksum exists (deduplication)
        String storageKey;
        
        
        var existingFile = fileRepository.findFirstByChecksumAndDeletedAtIsNull(checksum);
        boolean isDuplicate = existingFile.isPresent();

        if (isDuplicate) {
            storageKey = existingFile.get().getStorageKey();
        } else {
        // Store new file
        storageKey = storageService.store(multipartFile, userId, multipartFile.getOriginalFilename());
    }

        // Create file metadata
        File file = File.builder()
                .name(multipartFile.getOriginalFilename())
                .owner(user)
                .folder(folder)
                .size(multipartFile.getSize())
                .mimeType(multipartFile.getContentType())
                .storageKey(storageKey)
                .checksum(checksum)
                .version(1)
                .build();

        file = fileRepository.save(file);

        // Update user storage (only if not duplicate)
        if (!isDuplicate) {
            user.setStorageUsed(user.getStorageUsed() + multipartFile.getSize());
            userRepository.save(user);
        }

        // Log activity
        logActivity(user, ActivityType.UPLOAD, "FILE", file.getId());

        log.info("File uploaded: id={}, name={}, size={}", file.getId(), file.getName(), file.getSize());

        return FileUploadResponse.builder()
                .id(file.getId())
                .name(file.getName())
                .size(file.getSize())
                .mimeType(file.getMimeType())
                .version(file.getVersion())
                .createdAt(file.getCreatedAt())
                .folderId(folderId)
                .build();
    }

    @Transactional(readOnly = true)
    public List<FileInfoResponse> getUserFiles(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<File> files = fileRepository.findByOwnerAndDeletedAtIsNull(user);

        return files.stream()
                .map(this::mapToFileInfoResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public byte[] downloadFile(Long fileId, Long userId) throws IOException {
        File file = fileRepository.findByIdAndDeletedAtIsNull(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        // Check ownership
        if (!file.getOwner().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to file");
        }

        // Log activity
        logActivity(file.getOwner(), ActivityType.DOWNLOAD, "FILE", fileId);

        return storageService.getFile(file.getStorageKey());
    }

    @Transactional
    public void deleteFile(Long fileId, Long userId) throws IOException {
        File file = fileRepository.findByIdAndDeletedAtIsNull(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        // Check ownership
        if (!file.getOwner().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to file");
        }

        // Soft delete
        file.setDeletedAt(java.time.LocalDateTime.now());
        fileRepository.save(file);

        // Update user storage
        User user = file.getOwner();
        user.setStorageUsed(user.getStorageUsed() - file.getSize());
        userRepository.save(user);

        // Log activity
        logActivity(user, ActivityType.DELETE, "FILE", fileId);

        log.info("File deleted (soft): id={}", fileId);
    }

    private String calculateChecksum(InputStream inputStream) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            
            byte[] hashBytes = digest.digest();
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 algorithm not available", e);
        }
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

    private FileInfoResponse mapToFileInfoResponse(File file) {
        return FileInfoResponse.builder()
                .id(file.getId())
                .name(file.getName())
                .size(file.getSize())
                .mimeType(file.getMimeType())
                .version(file.getVersion())
                .createdAt(file.getCreatedAt())
                .updatedAt(file.getUpdatedAt())
                .folderId(file.getFolder() != null ? file.getFolder().getId() : null)
                .checksum(file.getChecksum())
                .build();
    }

    // Add method to get files in folder
    @Transactional(readOnly = true)
    public List<FileInfoResponse> getFilesInFolder(Long folderId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Folder folder = folderRepository.findByIdAndOwner(folderId, user)
                .orElseThrow(() -> new RuntimeException("Folder not found"));

        List<File> files = fileRepository.findByOwnerAndFolderAndDeletedAtIsNull(user, folder);

        return files.stream()
                .map(this::mapToFileInfoResponse)
                .collect(Collectors.toList());
    }
    // ========== FILE VERSIONING METHODS ==========

@Transactional
public FileUploadResponse updateFile(Long fileId, MultipartFile multipartFile, Long userId) throws IOException {
    // Get existing file
    File existingFile = fileRepository.findByIdAndDeletedAtIsNull(fileId)
            .orElseThrow(() -> new RuntimeException("File not found"));

    // Check ownership
    if (!existingFile.getOwner().getId().equals(userId)) {
        throw new RuntimeException("Unauthorized access to file");
    }

    User user = existingFile.getOwner();

    // Check storage quota for new version
    if (user.getStorageUsed() + multipartFile.getSize() > user.getStorageLimit()) {
        throw new RuntimeException("Storage quota exceeded");
    }

    // Save current version to history BEFORE updating
    FileVersion oldVersion = FileVersion.builder()
            .file(existingFile)
            .versionNumber(existingFile.getVersion())
            .storageKey(existingFile.getStorageKey())
            .size(existingFile.getSize())
            .build();
    fileVersionRepository.save(oldVersion);

    log.info("Saved version to history: fileId={}, version={}", fileId, existingFile.getVersion());

    // Calculate new checksum
    String checksum = calculateChecksum(multipartFile.getInputStream());

    // Store new file version
    String newStorageKey = storageService.store(multipartFile, userId, multipartFile.getOriginalFilename());

    // Update file metadata with new version
    long oldSize = existingFile.getSize();
    existingFile.setStorageKey(newStorageKey);
    existingFile.setSize(multipartFile.getSize());
    existingFile.setMimeType(multipartFile.getContentType());
    existingFile.setChecksum(checksum);
    existingFile.setVersion(existingFile.getVersion() + 1);
    existingFile.setUpdatedAt(java.time.LocalDateTime.now());

    existingFile = fileRepository.save(existingFile);

    // Update user storage (remove old size, add new size)
    user.setStorageUsed(user.getStorageUsed() - oldSize + multipartFile.getSize());
    userRepository.save(user);

    // Cleanup old versions (keep only last 5)
    cleanupOldVersions(existingFile);

    // Log activity
    logActivity(user, ActivityType.UPLOAD, "FILE", fileId);

    log.info("File updated: id={}, newVersion={}, oldSize={}, newSize={}", 
             fileId, existingFile.getVersion(), oldSize, multipartFile.getSize());

    return FileUploadResponse.builder()
            .id(existingFile.getId())
            .name(existingFile.getName())
            .size(existingFile.getSize())
            .mimeType(existingFile.getMimeType())
            .version(existingFile.getVersion())
            .createdAt(existingFile.getCreatedAt())
            .folderId(existingFile.getFolder() != null ? existingFile.getFolder().getId() : null)
            .build();
}

@Transactional(readOnly = true)
public List<FileVersionResponse> getFileVersions(Long fileId, Long userId) {
    File file = fileRepository.findByIdAndDeletedAtIsNull(fileId)
            .orElseThrow(() -> new RuntimeException("File not found"));

    // Check ownership
    if (!file.getOwner().getId().equals(userId)) {
        throw new RuntimeException("Unauthorized access to file");
    }

    // Get all historical versions
    List<FileVersion> versions = fileVersionRepository.findByFileOrderByVersionNumberDesc(file);

    return versions.stream()
            .map(v -> FileVersionResponse.builder()
                    .id(v.getId())
                    .versionNumber(v.getVersionNumber())
                    .size(v.getSize())
                    .createdAt(v.getCreatedAt())
                    .build())
            .collect(Collectors.toList());
}

@Transactional()
public byte[] downloadFileVersion(Long fileId, Integer versionNumber, Long userId) throws IOException {
    File file = fileRepository.findByIdAndDeletedAtIsNull(fileId)
            .orElseThrow(() -> new RuntimeException("File not found"));

    // Check ownership
    if (!file.getOwner().getId().equals(userId)) {
        throw new RuntimeException("Unauthorized access to file");
    }

    // If requesting current version, use current storageKey
    if (versionNumber.equals(file.getVersion())) {
        log.info("Downloading current version: fileId={}, version={}", fileId, versionNumber);
        return storageService.getFile(file.getStorageKey());
    }

    // Find historical version
    List<FileVersion> versions = fileVersionRepository.findByFileOrderByVersionNumberDesc(file);
    FileVersion targetVersion = versions.stream()
            .filter(v -> v.getVersionNumber().equals(versionNumber))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Version " + versionNumber + " not found"));

    // Log activity
    logActivity(file.getOwner(), ActivityType.DOWNLOAD, "FILE", fileId);

    log.info("Downloading historical version: fileId={}, version={}", fileId, versionNumber);
    return storageService.getFile(targetVersion.getStorageKey());
}

@Transactional
public FileUploadResponse restoreFileVersion(Long fileId, Integer versionNumber, Long userId) throws IOException {
    File file = fileRepository.findByIdAndDeletedAtIsNull(fileId)
            .orElseThrow(() -> new RuntimeException("File not found"));

    // Check ownership
    if (!file.getOwner().getId().equals(userId)) {
        throw new RuntimeException("Unauthorized access to file");
    }

    // Can't restore current version
    if (versionNumber.equals(file.getVersion())) {
        throw new RuntimeException("Cannot restore current version");
    }

    // Find target version to restore
    List<FileVersion> versions = fileVersionRepository.findByFileOrderByVersionNumberDesc(file);
    FileVersion targetVersion = versions.stream()
            .filter(v -> v.getVersionNumber().equals(versionNumber))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Version " + versionNumber + " not found"));

    // Save current version to history BEFORE restoring
    FileVersion currentAsVersion = FileVersion.builder()
            .file(file)
            .versionNumber(file.getVersion())
            .storageKey(file.getStorageKey())
            .size(file.getSize())
            .build();
    fileVersionRepository.save(currentAsVersion);

    log.info("Saved current version before restore: fileId={}, version={}", fileId, file.getVersion());

    // Restore old version as current
    User user = file.getOwner();
    long oldSize = file.getSize();

    file.setStorageKey(targetVersion.getStorageKey());
    file.setSize(targetVersion.getSize());
    file.setVersion(file.getVersion() + 1); // Increment version (restore = new version)
    file.setUpdatedAt(java.time.LocalDateTime.now());

    file = fileRepository.save(file);

    // Update user storage
    user.setStorageUsed(user.getStorageUsed() - oldSize + file.getSize());
    userRepository.save(user);

    // Log activity
    logActivity(user, ActivityType.RESTORE_VERSION, "FILE", fileId);

    log.info("Version restored: fileId={}, restoredVersion={}, newVersion={}", 
             fileId, versionNumber, file.getVersion());

    return FileUploadResponse.builder()
            .id(file.getId())
            .name(file.getName())
            .size(file.getSize())
            .mimeType(file.getMimeType())
            .version(file.getVersion())
            .createdAt(file.getCreatedAt())
            .folderId(file.getFolder() != null ? file.getFolder().getId() : null)
            .build();
}

/**
 * Delete old versions, keeping only the last 5
 */
private void cleanupOldVersions(File file) {
    List<FileVersion> versions = fileVersionRepository.findByFileOrderByVersionNumberDesc(file);
    
    // Keep only last 5 versions
    if (versions.size() > 5) {
        List<FileVersion> toDelete = versions.subList(5, versions.size());
        
        for (FileVersion version : toDelete) {
            try {
                // Delete from storage (S3 or local)
                storageService.delete(version.getStorageKey());
                // Delete from database
                fileVersionRepository.delete(version);
                log.info("Cleaned up old version: fileId={}, version={}", file.getId(), version.getVersionNumber());
            } catch (IOException e) {
                log.error("Failed to delete old version: fileId={}, version={}, error={}", 
                         file.getId(), version.getVersionNumber(), e.getMessage());
                // Don't throw exception, just log and continue
            }
        }
    }
}
@Transactional(readOnly = true)
public List<FileInfoResponse> searchFiles(String query, Long userId) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

    // Search by filename (case-insensitive)
    List<File> files = fileRepository.findByNameContainingIgnoreCaseAndOwnerAndDeletedAtIsNull(query, user);

    log.info("Search query='{}' returned {} results for user={}", query, files.size(), userId);

    return files.stream()
            .map(this::mapToFileInfoResponse)
            .collect(Collectors.toList());
}

@Transactional(readOnly = true)
public List<FileInfoResponse> searchFilesByType(String mimeType, Long userId) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

    // Search by MIME type
    List<File> files = fileRepository.findByMimeTypeContainingAndOwnerAndDeletedAtIsNull(mimeType, user);

    log.info("Search by type='{}' returned {} results for user={}", mimeType, files.size(), userId);

    return files.stream()
            .map(this::mapToFileInfoResponse)
            .collect(Collectors.toList());
}

@Transactional(readOnly = true)
public List<FileInfoResponse> getRecentFiles(Long userId, int limit) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

    // Get most recently uploaded files
    List<File> files = fileRepository.findByOwnerAndDeletedAtIsNullOrderByCreatedAtDesc(user);

    return files.stream()
            .limit(limit)
            .map(this::mapToFileInfoResponse)
            .collect(Collectors.toList());
}


}