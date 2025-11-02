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
public class PublicFileResponse {
    private String fileName;
    private Long size;
    private String mimeType;
    private String sharedByName;
    private LocalDateTime sharedAt;
    private LocalDateTime expiresAt;
    private boolean canDownload;
}