package com.example.backend.model;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 生理期记录实体类
 * 用于记录女性用户的生理期数据，支持实际记录和预测记录
 */
@Entity
@Table(name = "period_records")
@Data
public class PeriodRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 用户ID - 记录所属的用户
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    /**
     * 情侣ID - 用于数据隔离
     */
    @Column(name = "couple_id", nullable = false)
    private Long coupleId;
    
    /**
     * 生理期开始日期
     */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    /**
     * 生理期结束日期（可选，正在进行中的生理期可能没有结束日期）
     */
    @Column(name = "end_date")
    private LocalDate endDate;
    
    /**
     * 周期长度（天数）- 从上次生理期开始到这次开始的天数
     */
    @Column(name = "cycle_length")
    private Integer cycleLength;
    
    /**
     * 是否为预测记录
     * false: 用户手动记录的实际数据
     * true: 系统根据历史数据预测的记录
     */
    @Column(name = "is_predicted", nullable = false)
    private Boolean isPredicted = false;
    
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
     * 获取生理期持续天数
     */
    public Integer getDuration() {
        if (endDate == null) {
            return null;
        }
        return (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }
    
    /**
     * 检查生理期是否已结束
     */
    public boolean isEnded() {
        return endDate != null;
    }
    
    /**
     * 检查是否为实际记录（非预测）
     */
    public boolean isActual() {
        return !isPredicted;
    }
    
    /**
     * 检查生理期数据是否有效
     */
    public boolean isValid() {
        if (startDate == null) {
            return false;
        }
        
        if (endDate != null && endDate.isBefore(startDate)) {
            return false;
        }
        
        if (cycleLength != null && (cycleLength < 21 || cycleLength > 35)) {
            return false;
        }
        
        Integer duration = getDuration();
        if (duration != null && (duration < 4 || duration > 12)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 计算与另一个生理期记录的周期间隔
     */
    public Integer calculateCycleLengthFrom(PeriodRecord previousRecord) {
        if (previousRecord == null || previousRecord.getStartDate() == null) {
            return null;
        }
        
        return (int) ChronoUnit.DAYS.between(previousRecord.getStartDate(), this.startDate);
    }
}