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
public class FileVersionResponse {
    private Long id;
    private Integer versionNumber;
    private Long size;
    private LocalDateTime createdAt;
}