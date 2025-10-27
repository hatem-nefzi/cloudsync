package com.hatemnefzi.cloudsync.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FolderCreateRequest {
    
    @NotBlank(message = "Folder name is required")
    private String name;
    
    private Long parentId; // null for root folders
}