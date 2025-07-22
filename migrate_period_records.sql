-- ==========================================
-- 生理期记录表迁移脚本
-- ==========================================

USE love_db;

-- 创建生理期记录表
CREATE TABLE IF NOT EXISTS period_records (
    -- 主键ID，自动增长
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- 用户ID，不能为空
    user_id BIGINT NOT NULL,
    
    -- 情侣ID，用于数据隔离，不能为空
    couple_id BIGINT NOT NULL,
    
    -- 生理期开始日期，不能为空
    start_date DATE NOT NULL,
    
    -- 生理期结束日期，可以为空（正在进行中的生理期）
    end_date DATE,
    
    -- 周期长度（天数），从上次生理期开始到这次开始的天数
    cycle_length INT,
    
    -- 是否为预测记录，默认为false（实际记录）
    is_predicted BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- 创建时间
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- 更新时间
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- 是否已删除（软删除），默认为false
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- 创建索引优化查询性能
-- 为user_id和start_date创建组合索引（最常用的查询模式）
CREATE INDEX idx_user_date ON period_records(user_id, start_date);

-- 为couple_id和start_date创建组合索引（按情侣查询）
CREATE INDEX idx_couple_date ON period_records(couple_id, start_date);

-- 为is_predicted创建索引（区分实际记录和预测记录）
CREATE INDEX idx_predicted ON period_records(is_predicted);

-- 为is_deleted创建索引（软删除查询优化）
CREATE INDEX idx_deleted ON period_records(is_deleted);

-- 为user_id, is_predicted, is_deleted创建组合索引（预测算法查询优化）
CREATE INDEX idx_user_predicted_deleted ON period_records(user_id, is_predicted, is_deleted);

-- 添加数据约束
-- 确保结束日期不早于开始日期
ALTER TABLE period_records 
ADD CONSTRAINT chk_date_order 
CHECK (end_date IS NULL OR end_date >= start_date);

-- 确保周期长度在合理范围内（21-35天）
ALTER TABLE period_records 
ADD CONSTRAINT chk_cycle_length 
CHECK (cycle_length IS NULL OR (cycle_length >= 21 AND cycle_length <= 35));

-- 添加外键约束（如果需要）
-- ALTER TABLE period_records 
-- ADD CONSTRAINT fk_period_user 
-- FOREIGN KEY (user_id) REFERENCES users(id);

-- ALTER TABLE period_records 
-- ADD CONSTRAINT fk_period_couple 
-- FOREIGN KEY (couple_id) REFERENCES couples(id);

-- 插入示例数据（可选，用于测试）
-- INSERT INTO period_records (user_id, couple_id, start_date, end_date, cycle_length, is_predicted) VALUES
-- (1, 1, '2024-01-01', '2024-01-05', NULL, FALSE),
-- (1, 1, '2024-01-29', '2024-02-03', 28, FALSE),
-- (1, 1, '2024-02-26', '2024-03-02', 28, FALSE);