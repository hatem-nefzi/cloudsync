package com.hatemnefzi.cloudsync.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_versions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileVersion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private File file;
    
    @Column(nullable = false)
    private Integer versionNumber;
    
    @Column(nullable = false)
    private String storageKey;
    
    @Column(nullable = false)
    private Long size;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}