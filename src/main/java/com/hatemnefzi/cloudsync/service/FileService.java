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
}