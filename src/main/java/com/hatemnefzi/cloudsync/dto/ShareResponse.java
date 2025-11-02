package com.hatemnefzi.cloudsync.dto;

import com.hatemnefzi.cloudsync.entity.SharePermission;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareResponse {
    private Long id;
    private Long fileId;
    private String fileName;
    private Long folderId;
    private String folderName;
    private String sharedByEmail;
    private String sharedWithEmail; // null if public link
    private SharePermission permission;
    private String shareToken; // public link token
    private String shareUrl; // full URL for easy sharing
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private boolean isExpired;
}