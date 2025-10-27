package com.hatemnefzi.cloudsync.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileInfoResponse {
    private Long id;
    private String name;
    private Long size;
    private String mimeType;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long folderId;
    private String folderPath;
    private String checksum;
}