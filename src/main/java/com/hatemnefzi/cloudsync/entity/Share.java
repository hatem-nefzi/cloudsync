package com.hatemnefzi.cloudsync.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "shares")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Share {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private File file;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private Folder folder;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_by_id", nullable = false)
    private User sharedBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_with_id")
    private User sharedWith; // null if public link
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SharePermission permission; // VIEW, EDIT
    
    @Column(unique = true)
    private String shareToken; // For public links
    
    @Column
    private LocalDateTime expiresAt;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}