package com.example.backend.model;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 待办事项实体类
 * 情侣之间共享的待办事项列表（MVP版本）
 */
@Entity
@Table(name = "todo_items")
@Data
public class TodoItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 情侣ID
     */
    @Column(name = "couple_id", nullable = false)
    private Long coupleId;
    
    /**
     * 创建者用户ID
     */
    @Column(name = "creator_id", nullable = false)
    private Long creatorId;
    
    /**
     * 完成者用户ID（如果已完成）
     */
    @Column(name = "completer_id")
    private Long completerId;
    
    /**
     * 待办事项标题
     */
    @Column(nullable = false, length = 200)
    private String title;
    
    /**
     * 待办事项描述
     */
    @Column(length = 1000)
    private String description;
    
    /**
     * 状态：PENDING（待完成）, COMPLETED（已完成）
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;
    
    /**
     * 完成时间
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
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
     * 状态枚举
     */
    public enum Status {
        PENDING,    // 待完成
        COMPLETED   // 已完成
    }
    
    /**
     * 检查是否已完成
     */
    public boolean isCompleted() {
        return status == Status.COMPLETED;
    }
} 