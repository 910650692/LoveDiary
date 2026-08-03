package com.example.backend.model;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户动态实体类
 * 记录用户在系统中的各种操作和活动，用于生成动态时间线
 */
@Entity
@Table(name = "activity_logs")
@Data
public class Activity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 所属情侣ID
     */
    @Column(name = "couple_id", nullable = false)
    private Long coupleId;
    
    /**
     * 触发动态的用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    /**
     * 动态类型
     * TODO_COMPLETED - 完成愿望
     * TODO_CREATED - 创建愿望
     * PHOTO_UPLOADED - 上传照片
     * PHOTO_FAVORITED - 收藏照片
     * ANNIVERSARY_CREATED - 创建纪念日
     * ANNIVERSARY_REMINDER - 纪念日提醒
     * LOVE_MILESTONE - 恋爱里程碑
     * PERIOD_RECORD - 生理期记录
     * ALBUM_CREATED - 创建相册
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 50)
    private ActivityType activityType;
    
    /**
     * 动态标题
     */
    @Column(name = "title", nullable = false, length = 200)
    private String title;
    
    /**
     * 动态描述
     */
    @Column(name = "description", length = 500)
    private String description;
    
    /**
     * 关联的实体ID（如TodoItem的ID、Photo的ID等）
     */
    @Column(name = "reference_id")
    private Long referenceId;
    
    /**
     * 关联的实体类型
     */
    @Column(name = "reference_type", length = 50)
    private String referenceType;
    
    /**
     * 额外的元数据（JSON格式）
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
    
    /**
     * 动态的图标或emoji
     */
    @Column(name = "icon", length = 10)
    private String icon;
    
    /**
     * 动态创建时间
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    /**
     * 是否已删除（软删除）
     */
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
    
    /**
     * 删除时间
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    // 构造函数
    public Activity() {
        this.createdAt = LocalDateTime.now();
        this.isDeleted = false;
    }
    
    public Activity(Long coupleId, Long userId, ActivityType activityType, String title) {
        this();
        this.coupleId = coupleId;
        this.userId = userId;
        this.activityType = activityType;
        this.title = title;
    }
    
    public Activity(Long coupleId, Long userId, ActivityType activityType, String title, String description) {
        this(coupleId, userId, activityType, title);
        this.description = description;
    }
    
    /**
     * 动态类型枚举
     */
    public enum ActivityType {
        TODO_COMPLETED("完成愿望", "💕"),
        TODO_CREATED("创建愿望", "✨"),
        PHOTO_UPLOADED("上传照片", "📸"),
        PHOTO_FAVORITED("收藏照片", "💖"),
        ANNIVERSARY_CREATED("创建纪念日", "🎂"),
        ANNIVERSARY_REMINDER("纪念日提醒", "⏰"),
        LOVE_MILESTONE("恋爱里程碑", "🌟"),
        PERIOD_RECORD("生理期记录", "🌙"),
        ALBUM_CREATED("创建相册", "📁"),
        USER_JOINED("加入情侣", "💑"),
        LOVE_DATE_SET("设置恋爱日期", "💝");
        
        private final String displayName;
        private final String defaultIcon;
        
        ActivityType(String displayName, String defaultIcon) {
            this.displayName = displayName;
            this.defaultIcon = defaultIcon;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getDefaultIcon() {
            return defaultIcon;
        }
    }
    
    // 便捷方法：设置图标
    public void setIconFromType() {
        if (this.activityType != null) {
            this.icon = this.activityType.getDefaultIcon();
        }
    }
    
    // 便捷方法：设置引用
    public void setReference(Long referenceId, String referenceType) {
        this.referenceId = referenceId;
        this.referenceType = referenceType;
    }
    
    // 便捷方法：软删除
    public void markAsDeleted() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}