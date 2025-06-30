package com.example.backend.model;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDate;

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
}
