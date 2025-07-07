package com.example.backend.model;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalDate;

/**
 * 情侣实体类
 * 当两个用户通过邀请码匹配成功后，创建情侣记录
 */
@Entity
@Table(name = "couples")
@Data
public class Couple {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 用户1的ID
     */
    @Column(name = "user1_id", nullable = false)
    private Long user1Id;
    
    /**
     * 用户2的ID
     */
    @Column(name = "user2_id", nullable = false)
    private Long user2Id;
    

    
    /**
     * 恋爱开始日期
     */
    @Column(name = "love_start_date")
    private LocalDate loveStartDate;
    
    /**
     * 匹配日期（情侣关系建立的时间）
     */
    @Column(name = "match_date")
    private LocalDateTime matchDate;
    
    /**
     * 情侣状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CoupleStatus status = CoupleStatus.ACTIVE;
    

    
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
     * 情侣状态枚举
     */
    public enum CoupleStatus {
        ACTIVE,     // 激活 - 关系正常
        INACTIVE    // 未激活 - 关系结束或暂停
    }
    
    /**
     * 检查情侣关系是否正常
     */
    public boolean isActive() {
        return status == CoupleStatus.ACTIVE && !isDeleted;
    }
    
    /**
     * 检查用户是否属于这个情侣
     */
    public boolean containsUser(Long userId) {
        return user1Id.equals(userId) || user2Id.equals(userId);
    }
    
    /**
     * 获取另一半的用户ID
     */
    public Long getPartnerUserId(Long userId) {
        if (user1Id.equals(userId)) {
            return user2Id;
        } else if (user2Id.equals(userId)) {
            return user1Id;
        }
        return null;
    }
} 