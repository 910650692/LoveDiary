package com.example.backend.model;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户实体类
 * 每个用户注册后都有自己的邀请码，可以通过邀请码与其他用户匹配成情侣
 */
@Entity
@Table(name = "users")
@Data
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 用户名（用于登录）
     */
    @Column(nullable = false, unique = true, length = 50)
    private String username;
    
    /**
     * 密码（应该加密存储）
     */
    @Column(nullable = false, length = 255)
    private String password;
    
    /**
     * 昵称（显示名称）
     */
    @Column(nullable = false, length = 50)
    private String nickname;
    
    /**
     * 邮箱
     */
    @Column(unique = true, length = 100)
    private String email;
    
    /**
     * 手机号
     */
    @Column(length = 20)
    private String phone;
    
    /**
     * 邀请码 - 注册成功后生成的6位字母数字组合
     * 其他用户可以通过这个邀请码与当前用户匹配
     */
    @Column(name = "invitation_code", nullable = false, unique = true, length = 6)
    private String invitationCode;
    
    /**
     * 用户状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.SINGLE;
    
    /**
     * 情侣ID - 如果已匹配，指向couple表的ID
     */
    @Column(name = "couple_id")
    private Long coupleId;
    
    /**
     * 性别
     */
    @Enumerated(EnumType.STRING)
    private Gender gender;
    
    /**
     * 生日
     */
    @Column(name = "birth_date")
    private java.time.LocalDate birthDate;
    
    /**
     * 头像URL
     */
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;
    

    
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
     * 用户状态枚举
     */
    public enum UserStatus {
        SINGLE,    // 单身 - 可以接受匹配邀请
        MATCHED    // 已匹配 - 已经有情侣了
    }
    
    /**
     * 性别枚举
     */
    public enum Gender {
        MALE,      // 男性
        FEMALE,    // 女性
        OTHER      // 其他
    }
    
    /**
     * 检查用户是否可以进行匹配
     */
    public boolean canMatch() {
        return status == UserStatus.SINGLE && !isDeleted;
    }
    
    /**
     * 检查用户是否已经匹配
     */
    public boolean isMatched() {
        return status == UserStatus.MATCHED && coupleId != null;
    }
} 