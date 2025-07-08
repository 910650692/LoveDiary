package com.example.backend.model;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 相册实体类（MVP版本）
 * 情侣之间的相册管理
 */
@Entity
@Table(name = "albums")
@Data
public class Album {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 情侣ID
     */
    @Column(name = "couple_id", nullable = false)
    private Long coupleId;
    
    /**
     * 相册名称
     */
    @Column(nullable = false, length = 100)
    private String name;
    
    /**
     * 相册描述
     */
    @Column(length = 500)
    private String description;
    
    /**
     * 封面照片ID
     */
    @Column(name = "cover_photo_id")
    private Long coverPhotoId;
    
    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * 是否已删除（软删除）
     */
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
} 