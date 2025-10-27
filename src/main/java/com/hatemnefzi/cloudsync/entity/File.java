package com.hatemnefzi.cloudsync.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "files")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class File {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private Folder folder;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
    
    @Column(nullable = false)
    private Long size; // in bytes
    
    @Column
    private String mimeType;
    
    @Column(nullable = false)
    private String storageKey; // S3 key or local path
    
    @Column
    private String checksum; // SHA-256 for deduplication
    
    @Column(nullable = false)
    private Integer version = 1;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @Column
    private LocalDateTime deletedAt; // Soft delete
}