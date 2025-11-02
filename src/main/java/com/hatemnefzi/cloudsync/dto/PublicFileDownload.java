package com.hatemnefzi.cloudsync.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicFileDownload {
    private byte[] fileData;
    private String fileName;
    private String mimeType;
}