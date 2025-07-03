-- ==========================================
-- 恋爱日记数据库建表脚本
-- ==========================================

-- 1. 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS love_db 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- 2. 使用数据库
USE love_db;

-- 3. 创建纪念日表
CREATE TABLE IF NOT EXISTS anniversaries (
    -- 主键ID，自动增长
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- 纪念日名称，最大50字符，不能为空
    name VARCHAR(50) NOT NULL,
    
    -- 纪念日日期，不能为空
    date DATE NOT NULL,
    
    -- 备注信息，最大500字符，可以为空
    notes VARCHAR(500),
    
    -- 情侣ID，用于数据隔离，不能为空
    couple_id BIGINT NOT NULL,
    
    -- 是否启用推送通知，默认为true
    enable_notification BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- 创建时间（可选，Spring Boot会自动管理）
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- 更新时间（可选，Spring Boot会自动管理）
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 4. 创建索引优化查询性能
-- 为couple_id创建索引（经常按情侣查询）
CREATE INDEX idx_couple_id ON anniversaries(couple_id);

-- 为date创建索引（经常按日期查询）
CREATE INDEX idx_date ON anniversaries(date);

-- 为couple_id和date创建组合索引
CREATE INDEX idx_couple_date ON anniversaries(couple_id, date);

-- 为推送查询创建索引
CREATE INDEX idx_couple_notification ON anniversaries(couple_id, enable_notification);

-- 5. 插入一些测试数据
INSERT INTO anniversaries (name, date, notes, couple_id, enable_notification) VALUES 
('初次相遇', '2023-05-20', '在咖啡厅第一次见面，那是一个阳光明媚的下午', 1, TRUE),
('第一次约会', '2023-06-14', '一起看了电影，牵手漫步在公园里', 1, FALSE),
('正式交往', '2023-07-01', '在星空下许下承诺，从朋友变成恋人', 1, TRUE),
('小王的生日', '1995-08-15', '记住这个重要的日子', 1, TRUE),
('小李的生日', '1996-12-03', '记住这个重要的日子', 1, TRUE);

-- 6. 查看创建的表结构
DESCRIBE anniversaries;

-- 7. 查看插入的测试数据
SELECT * FROM anniversaries;

-- ==========================================
-- 使用说明：
-- 1. 连接到MySQL数据库
-- 2. 执行这个SQL脚本
-- 3. 启动Spring Boot应用，它会自动连接到这个数据库
-- ========================================== 