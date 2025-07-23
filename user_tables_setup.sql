-- ==========================================
-- 用户系统数据库建表脚本
-- ==========================================

-- 使用现有数据库
USE love_db;

-- 创建用户表
CREATE TABLE IF NOT EXISTS users (
    -- 主键ID，自动增长
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- 用户名（用于登录），唯一，不能为空
    username VARCHAR(50) NOT NULL UNIQUE,
    
    -- 密码（加密后存储），不能为空
    password VARCHAR(255) NOT NULL,
    
    -- 昵称（显示名称），不能为空
    nickname VARCHAR(50) NOT NULL,
    
    -- 邮箱，唯一，可以为空
    email VARCHAR(100) UNIQUE,
    
    -- 手机号，可以为空
    phone VARCHAR(20),
    
    -- 邀请码，6位字母数字组合，唯一，不能为空
    invitation_code VARCHAR(6) NOT NULL UNIQUE,
    
    -- 用户状态：SINGLE(单身), MATCHED(已匹配)
    status ENUM('SINGLE', 'MATCHED') NOT NULL DEFAULT 'SINGLE',
    
    -- 情侣ID，如果已匹配，指向couples表的ID
    couple_id BIGINT,
    
    -- 性别：MALE(男性), FEMALE(女性), OTHER(其他)
    gender ENUM('MALE', 'FEMALE', 'OTHER'),
    
    -- 生日
    birth_date DATE,
    
    -- 头像URL
    avatar_url VARCHAR(500),
    

    
    -- 创建时间
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- 更新时间
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- 是否已删除（软删除）
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- 创建情侣表
CREATE TABLE IF NOT EXISTS couples (
    -- 主键ID，自动增长
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- 用户1的ID
    user1_id BIGINT NOT NULL,
    
    -- 用户2的ID
    user2_id BIGINT NOT NULL,
    

    
    -- 恋爱开始日期
    anniversary_date DATE,
    
    -- 情侣状态：ACTIVE(激活), INACTIVE(未激活)
    status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    

    
    -- 创建时间
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- 更新时间
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- 是否已删除（软删除）
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- 创建索引优化查询性能

-- 用户表索引
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_invitation_code ON users(invitation_code);
CREATE INDEX idx_users_couple_id ON users(couple_id);
CREATE INDEX idx_users_status ON users(status);

-- 情侣表索引
CREATE INDEX idx_couples_user1_id ON couples(user1_id);
CREATE INDEX idx_couples_user2_id ON couples(user2_id);
CREATE INDEX idx_couples_status ON couples(status);

-- 外键约束（可选）
-- ALTER TABLE users ADD FOREIGN KEY (couple_id) REFERENCES couples(id);
-- ALTER TABLE couples ADD FOREIGN KEY (user1_id) REFERENCES users(id);
-- ALTER TABLE couples ADD FOREIGN KEY (user2_id) REFERENCES users(id);

-- 插入测试数据
INSERT INTO users (username, password, nickname, email, invitation_code, status, gender) VALUES 
('testuser1', 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855', '测试用户1', 'test1@example.com', 'ABC123', 'SINGLE', 'MALE'),
('testuser2', 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855', '测试用户2', 'test2@example.com', 'DEF456', 'SINGLE', 'FEMALE');

-- 查看创建的表结构
DESCRIBE users;
DESCRIBE couples;

-- 查看插入的测试数据
SELECT id, username, nickname, invitation_code, status FROM users;

-- ==========================================
-- 使用说明：
-- 1. 用户注册时会生成唯一的6位邀请码
-- 2. 其他用户可以通过邀请码进行匹配
-- 3. 匹配成功后会创建couples记录
-- 4. 用户状态会从SINGLE变为MATCHED
-- ========================================== 