package com.hatemnefzi.cloudsync.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

import java.time.LocalDateTime;

import com.hatemnefzi.cloudsync.entity.SharePermission;

@Data
public class ShareCreateRequest {
    
    private Long fileId;
    private Long folderId; // Either fileId or folderId, not both
    
    @Email
    private String sharedWithEmail; // null for public link
    
    private SharePermission permission; // VIEW or EDIT
    
    private LocalDateTime expiresAt; // null = never expires
    
    private boolean isPublic; // true = generate public link
}