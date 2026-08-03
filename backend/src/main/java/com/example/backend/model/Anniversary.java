package com.example.backend.model;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 纪念日实体类 - 简单版本
 * 这是我们的数据模型，对应数据库中的一张表
 */
@Entity  // 告诉Spring这是一个数据库实体
@Table(name = "anniversaries")  // 指定数据库表名
@Data    // Lombok注解，自动生成getter/setter/toString等方法
public class Anniversary {
    
    @Id  // 标记这是主键
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // 主键自动增长
    private Long id;
    
    @Column(nullable = false, length = 50)  // 数据库字段，不能为空，最大50字符
    private String name;
    
    @Column(nullable = false)  // 不能为空
    private LocalDate date;
    
    @Column(length = 500)  // 最大500字符，可以为空
    private String notes;
    
    @Column(name = "couple_id", nullable = false)  // 指定数据库字段名
    private Long coupleId;
    
    /**
     * 是否启用推送通知
     * true: 需要推送通知（如生日、恋爱纪念日等重要事件）
     * false: 不需要推送通知（如第一次做某事等普通纪念日）
     * 默认值为true，保证重要纪念日不会被遗漏
     */
    @Column(name = "enable_notification", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean enableNotification = true;
    
    // 临时手动添加的setter方法（当Lombok生效后可以删除）
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public void setDate(LocalDate date) {
        this.date = date;
    }
    
    public LocalDate getDate() {
        return date;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setCoupleId(Long coupleId) {
        this.coupleId = coupleId;
    }
    
    public Long getCoupleId() {
        return coupleId;
    }
    
    public void setEnableNotification(Boolean enableNotification) {
        this.enableNotification = enableNotification;
    }
    
    public Boolean getEnableNotification() {
        return enableNotification;
    }
    
    // ========== 新增：计算纪念日相关的方法 ==========
    
    /**
     * 计算距离下次纪念日还有多少天
     * 如果今年的纪念日已过，则计算到明年的纪念日
     * 
     * @return 天数（0表示今天就是纪念日）
     */
    public long getDaysUntilNext() {
        LocalDate today = LocalDate.now();
        
        // 计算今年的纪念日
        LocalDate thisYearAnniversary = this.date.withYear(today.getYear());
        
        // 如果今年的纪念日已经过了，就计算明年的
        if (thisYearAnniversary.isBefore(today)) {
            thisYearAnniversary = thisYearAnniversary.plusYears(1);
        }
        
        return ChronoUnit.DAYS.between(today, thisYearAnniversary);
    }
    
    /**
     * 计算已经度过了多少天
     * 从第一个纪念日到现在
     * 
     * @return 已过天数
     */
    public long getDaysPassed() {
        LocalDate today = LocalDate.now();
        return ChronoUnit.DAYS.between(this.date, today);
    }
    
    /**
     * 计算已经度过了多少年
     * 
     * @return 已过年数
     */
    public long getYearsPassed() {
        LocalDate today = LocalDate.now();
        return ChronoUnit.YEARS.between(this.date, today);
    }
    
    /**
     * 判断今天是否是纪念日
     * 
     * @return true表示今天是纪念日
     */
    public boolean isToday() {
        LocalDate today = LocalDate.now();
        return this.date.getMonthValue() == today.getMonthValue() 
            && this.date.getDayOfMonth() == today.getDayOfMonth();
    }
    
    /**
     * 获取下次纪念日的完整日期
     * 
     * @return 下次纪念日的日期
     */
    public LocalDate getNextAnniversaryDate() {
        LocalDate today = LocalDate.now();
        LocalDate thisYearAnniversary = this.date.withYear(today.getYear());
        
        if (thisYearAnniversary.isBefore(today)) {
            return thisYearAnniversary.plusYears(1);
        }
        
        return thisYearAnniversary;
    }
}
