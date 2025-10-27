package com.hatemnefzi.cloudsync.service;

import com.hatemnefzi.cloudsync.dto.FolderCreateRequest;
import com.hatemnefzi.cloudsync.dto.FolderResponse;
import com.hatemnefzi.cloudsync.entity.Activity;
import com.hatemnefzi.cloudsync.entity.ActivityType;
import com.hatemnefzi.cloudsync.entity.Folder;
import com.hatemnefzi.cloudsync.entity.User;
import com.hatemnefzi.cloudsync.repository.ActivityRepository;
import com.hatemnefzi.cloudsync.repository.FileRepository;
import com.hatemnefzi.cloudsync.repository.FolderRepository;
import com.hatemnefzi.cloudsync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FolderService {

    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final ActivityRepository activityRepository;

    @Transactional
    public FolderResponse createFolder(FolderCreateRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Folder parent = null;
        String path;

        if (request.getParentId() != null) {
            // Create subfolder
            parent = folderRepository.findByIdAndOwner(request.getParentId(), user)
                    .orElseThrow(() -> new RuntimeException("Parent folder not found"));
            path = parent.getPath() + "/" + request.getName();
        } else {
            // Create root folder
            path = "/" + request.getName();
        }

        // Check if folder with same name exists in same location
        List<Folder> existingFolders = parent != null 
            ? folderRepository.findByOwnerAndParent(user, parent)
            : folderRepository.findByOwnerAndParentIsNull(user);

        boolean nameExists = existingFolders.stream()
                .anyMatch(f -> f.getName().equals(request.getName()));

        if (nameExists) {
            throw new RuntimeException("Folder with this name already exists in this location");
        }

        Folder folder = Folder.builder()
                .name(request.getName())
                .parent(parent)
                .owner(user)
                .path(path)
                .updatedAt(LocalDateTime.now())
                .build();

        folder = folderRepository.save(folder);

        // Log activity
        logActivity(user, ActivityType.CREATE_FOLDER, "FOLDER", folder.getId());

        log.info("Folder created: id={}, name={}, path={}", folder.getId(), folder.getName(), folder.getPath());

        return mapToFolderResponse(folder, user);
    }

    @Transactional(readOnly = true)
    public List<FolderResponse> getRootFolders(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Folder> rootFolders = folderRepository.findByOwnerAndParentIsNull(user);
        
        return rootFolders.stream()
                .map(folder -> mapToFolderResponse(folder, user))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FolderResponse> getSubfolders(Long folderId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Folder parent = folderRepository.findByIdAndOwner(folderId, user)
                .orElseThrow(() -> new RuntimeException("Folder not found"));

        List<Folder> subfolders = folderRepository.findByOwnerAndParent(user, parent);

        return subfolders.stream()
                .map(folder -> mapToFolderResponse(folder, user))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FolderResponse getFolderTree(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Build complete folder tree
        List<Folder> rootFolders = folderRepository.findByOwnerAndParentIsNull(user);

        // Virtual root
        FolderResponse virtualRoot = FolderResponse.builder()
                .id(null)
                .name("My Drive")
                .path("/")
                .subfolders(rootFolders.stream()
                        .map(folder -> buildFolderTree(folder, user))
                        .collect(Collectors.toList()))
                .build();

        return virtualRoot;
    }

    @Transactional
    public void deleteFolder(Long folderId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Folder folder = folderRepository.findByIdAndOwner(folderId, user)
                .orElseThrow(() -> new RuntimeException("Folder not found"));

        // TODO: Also delete all files in folder and subfolders (cascade delete)
        // For now, simple delete
        folderRepository.delete(folder);

        // Log activity
        logActivity(user, ActivityType.DELETE, "FOLDER", folderId);

        log.info("Folder deleted: id={}", folderId);
    }

    @Transactional
    public FolderResponse renameFolder(Long folderId, String newName, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Folder folder = folderRepository.findByIdAndOwner(folderId, user)
                .orElseThrow(() -> new RuntimeException("Folder not found"));

        // Update name and path
        String oldPath = folder.getPath();
        String newPath;

        if (folder.getParent() != null) {
            newPath = folder.getParent().getPath() + "/" + newName;
        } else {
            newPath = "/" + newName;
        }

        folder.setName(newName);
        folder.setPath(newPath);
        folder.setUpdatedAt(LocalDateTime.now());

        // TODO: Update paths of all subfolders recursively
        folder = folderRepository.save(folder);

        // Log activity
        logActivity(user, ActivityType.RENAME, "FOLDER", folderId);

        log.info("Folder renamed: id={}, oldName={}, newName={}", folderId, oldPath, newPath);

        return mapToFolderResponse(folder, user);
    }

    private FolderResponse buildFolderTree(Folder folder, User user) {
        List<Folder> children = folderRepository.findByOwnerAndParent(user, folder);
        
        List<FolderResponse> childResponses = children.stream()
                .map(child -> buildFolderTree(child, user))
                .collect(Collectors.toList());

        int fileCount = fileRepository.findByOwnerAndFolderAndDeletedAtIsNull(user, folder).size();

        return FolderResponse.builder()
                .id(folder.getId())
                .name(folder.getName())
                .path(folder.getPath())
                .parentId(folder.getParent() != null ? folder.getParent().getId() : null)
                .createdAt(folder.getCreatedAt())
                .updatedAt(folder.getUpdatedAt())
                .subfolders(childResponses)
                .fileCount(fileCount)
                .build();
    }

    private FolderResponse mapToFolderResponse(Folder folder, User user) {
        int fileCount = fileRepository.findByOwnerAndFolderAndDeletedAtIsNull(user, folder).size();

        return FolderResponse.builder()
                .id(folder.getId())
                .name(folder.getName())
                .path(folder.getPath())
                .parentId(folder.getParent() != null ? folder.getParent().getId() : null)
                .createdAt(folder.getCreatedAt())
                .updatedAt(folder.getUpdatedAt())
                .subfolders(new ArrayList<>()) // Don't load by default
                .fileCount(fileCount)
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