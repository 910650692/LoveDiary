package com.example.backend.model;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 照片实体类（MVP版本）
 * 情侣之间的照片管理
 */
@Entity
@Table(name = "photos")
@Data
public class Photo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 情侣ID
     */
    @Column(name = "couple_id", nullable = false)
    private Long coupleId;
    
    /**
     * 相册ID（可以为空，表示未分类照片）
     */
    @Column(name = "album_id")
    private Long albumId;
    
    /**
     * 创建者用户ID
     */
    @Column(name = "creator_id", nullable = false)
    private Long creatorId;
    
    /**
     * 文件名（存储路径）
     */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;
    
    /**
     * 原始文件名
     */
    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;
    
    /**
     * 文件路径
     */
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;
    
    /**
     * 文件大小（字节）
     */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;
    
    /**
     * 文件类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false)
    private FileType fileType = FileType.PHOTO;
    
    /**
     * MIME类型
     */
    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;
    
    /**
     * 图片宽度（像素）
     */
    @Column(name = "width")
    private Integer width;
    
    /**
     * 图片高度（像素）
     */
    @Column(name = "height")
    private Integer height;
    
    /**
     * 视频时长（秒）
     */
    @Column(name = "duration")
    private Integer duration;
    
    /**
     * 照片描述
     */
    @Column(length = 500)
    private String description;
    
    /**
     * 拍摄地点
     */
    @Column(length = 200)
    private String location;
    
    /**
     * 标签（用逗号分隔）
     */
    @Column(length = 500)
    private String tags;
    
    /**
     * 是否收藏
     */
    @Column(name = "is_favorite", nullable = false)
    private Boolean isFavorite = false;
    
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
    
    /**
     * 文件类型枚举
     */
    public enum FileType {
        PHOTO,  // 照片
        VIDEO   // 视频
    }
    
    /**
     * 获取标签数组
     */
    public String[] getTagArray() {
        if (tags == null || tags.trim().isEmpty()) {
            return new String[0];
        }
        return tags.split(",");
    }
    
    /**
     * 设置标签数组
     */
    public void setTagArray(String[] tagArray) {
        if (tagArray == null || tagArray.length == 0) {
            this.tags = null;
        } else {
            this.tags = String.join(",", tagArray);
        }
    }
    
    /**
     * 检查是否为照片
     */
    public boolean isPhoto() {
        return fileType == FileType.PHOTO;
    }
    
    /**
     * 检查是否为视频
     */
    public boolean isVideo() {
        return fileType == FileType.VIDEO;
    }
} 