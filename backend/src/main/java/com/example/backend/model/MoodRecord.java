package com.example.backend.model;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 情绪记录实体类
 */
@Entity
@Table(name = "mood_records")
@Data
public class MoodRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "couple_id", nullable = false)
    private Long coupleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "mood_type", nullable = false)
    private MoodType moodType;

    @Column(name = "mood_level")
    private Integer moodLevel = 3;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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
     * 情绪类型枚举
     */
    public enum MoodType {
        HAPPY,      // 😊 开心
        SAD,        // 😢 难过
        ANGRY,      // 😠 生气
        LOVE,       // 😍 甜蜜
        MISS,       // 🥺 想念
        NEUTRAL,    // 😐 平淡
        ANXIOUS,    // 😰 焦虑
        EXCITED     // 🤩 兴奋
    }
}
