package com.hatemnefzi.cloudsync.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "activities")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Activity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityType action;
    
    @Column
    private String entityType; // FILE, FOLDER
    
    @Column
    private Long entityId;
    
    @Column
    private String details; // JSON string with extra info
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}