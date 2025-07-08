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

-- 7. 创建待办事项表（MVP版本）
CREATE TABLE IF NOT EXISTS todo_items (
    -- 主键ID，自动增长
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- 情侣ID，用于数据隔离，不能为空
    couple_id BIGINT NOT NULL,
    
    -- 创建者用户ID，不能为空
    creator_id BIGINT NOT NULL,
    
    -- 完成者用户ID（如果已完成），可以为空
    completer_id BIGINT,
    
    -- 待办事项标题，最大200字符，不能为空
    title VARCHAR(200) NOT NULL,
    
    -- 待办事项描述，最大1000字符，可以为空
    description VARCHAR(1000),
    
    -- 状态：PENDING（待完成）, COMPLETED（已完成）
    status ENUM('PENDING', 'COMPLETED') NOT NULL DEFAULT 'PENDING',
    
    -- 完成时间，可以为空
    completed_at DATETIME,
    
    -- 创建时间
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- 更新时间
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- 是否已删除（软删除），默认为false
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- 8. 为待办事项表创建索引（MVP版本）
-- 为couple_id创建索引（经常按情侣查询）
CREATE INDEX idx_todo_couple_id ON todo_items(couple_id);

-- 为creator_id创建索引（经常按创建者查询）
CREATE INDEX idx_todo_creator_id ON todo_items(creator_id);

-- 为status创建索引（经常按状态查询）
CREATE INDEX idx_todo_status ON todo_items(status);

-- 为软删除查询创建索引
CREATE INDEX idx_todo_deleted ON todo_items(is_deleted);

-- 为couple_id和status创建组合索引
CREATE INDEX idx_todo_couple_status ON todo_items(couple_id, status);

-- 9. 插入一些测试待办事项数据（MVP版本）
INSERT INTO todo_items (couple_id, creator_id, title, description, status) VALUES 
(1, 1, '买生日礼物', '为另一半准备生日礼物，要用心挑选', 'PENDING'),
(1, 2, '准备约会计划', '计划一次浪漫的约会，包括餐厅和活动', 'PENDING'),
(1, 1, '整理照片', '整理我们的合照，制作相册', 'PENDING'),
(1, 2, '学习新技能', '一起学习烹饪，为对方做一顿饭', 'PENDING'),
(1, 1, '制定旅行计划', '计划一次短途旅行，放松心情', 'COMPLETED');

-- 10. 查看创建的表结构
DESCRIBE todo_items;

-- 11. 查看插入的测试数据
SELECT * FROM todo_items;

-- 12. 创建相册表（MVP版本）
CREATE TABLE IF NOT EXISTS albums (
    -- 主键ID，自动增长
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- 情侣ID，用于数据隔离，不能为空
    couple_id BIGINT NOT NULL,
    
    -- 相册名称，最大100字符，不能为空
    name VARCHAR(100) NOT NULL,
    
    -- 相册描述，最大500字符，可以为空
    description VARCHAR(500),
    
    -- 封面照片ID，可以为空
    cover_photo_id BIGINT,
    
    -- 创建时间
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    -- 更新时间
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    
    -- 是否已删除（软删除），默认为false
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- 13. 创建照片表（MVP版本）
CREATE TABLE IF NOT EXISTS photos (
    -- 主键ID，自动增长
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- 情侣ID，用于数据隔离，不能为空
    couple_id BIGINT NOT NULL,
    
    -- 相册ID，可以为空（未分类照片）
    album_id BIGINT,
    
    -- 创建者用户ID，不能为空
    creator_id BIGINT NOT NULL,
    
    -- 文件名（存储路径）
    file_name VARCHAR(255) NOT NULL,
    
    -- 原始文件名
    original_name VARCHAR(255) NOT NULL,
    
    -- 文件路径
    file_path VARCHAR(500) NOT NULL,
    
    -- 文件大小（字节）
    file_size BIGINT NOT NULL,
    
    -- 文件类型（PHOTO, VIDEO）
    file_type ENUM('PHOTO', 'VIDEO') NOT NULL DEFAULT 'PHOTO',
    
    -- MIME类型
    mime_type VARCHAR(100) NOT NULL,
    
    -- 图片宽度（像素）
    width INT,
    
    -- 图片高度（像素）
    height INT,
    
    -- 视频时长（秒）
    duration INT,
    
    -- 照片描述，最大500字符，可以为空
    description VARCHAR(500),
    
    -- 拍摄地点，最大200字符，可以为空
    location VARCHAR(200),
    
    -- 标签（用逗号分隔），最大500字符，可以为空
    tags VARCHAR(500),
    
    -- 是否收藏，默认为false
    is_favorite BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- 创建时间
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    -- 更新时间
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    
    -- 是否已删除（软删除），默认为false
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- 14. 为相册表创建索引
CREATE INDEX idx_album_couple_id ON albums(couple_id);
CREATE INDEX idx_album_deleted ON albums(is_deleted);
CREATE INDEX idx_album_created_at ON albums(created_at);

-- 15. 为照片表创建索引
CREATE INDEX idx_photo_couple_id ON photos(couple_id);
CREATE INDEX idx_photo_album_id ON photos(album_id);
CREATE INDEX idx_photo_creator_id ON photos(creator_id);
CREATE INDEX idx_photo_file_type ON photos(file_type);
CREATE INDEX idx_photo_favorite ON photos(is_favorite);
CREATE INDEX idx_photo_deleted ON photos(is_deleted);
CREATE INDEX idx_photo_created_at ON photos(created_at);
CREATE INDEX idx_photo_couple_created ON photos(couple_id, created_at);
CREATE INDEX idx_photo_couple_favorite ON photos(couple_id, is_favorite);

-- 16. 插入一些测试相册数据
INSERT INTO albums (couple_id, name, description) VALUES 
(1, '我们的第一次约会', '记录我们第一次约会的美好时光'),
(1, '旅行回忆', '我们一起旅行的照片'),
(1, '日常生活', '记录我们的日常生活点滴');

-- 17. 查看创建的表结构
DESCRIBE albums;
DESCRIBE photos;

-- 18. 查看插入的测试数据
SELECT * FROM anniversaries;
SELECT * FROM albums;

-- 标签表
CREATE TABLE IF NOT EXISTS tags (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    couple_id BIGINT NOT NULL,
    name VARCHAR(32) NOT NULL,
    color VARCHAR(16) NOT NULL DEFAULT '#FFB300',
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted TINYINT(1) DEFAULT 0,
    UNIQUE KEY uk_couple_name (couple_id, name, is_deleted),
    FOREIGN KEY (couple_id) REFERENCES couples(id),
    FOREIGN KEY (created_by) REFERENCES users(id)
);

-- 照片-标签关系表
CREATE TABLE IF NOT EXISTS photo_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    photo_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    FOREIGN KEY (photo_id) REFERENCES photos(id),
    FOREIGN KEY (tag_id) REFERENCES tags(id),
    UNIQUE KEY uk_photo_tag (photo_id, tag_id)
);

-- ==========================================
-- 使用说明：
-- 1. 连接到MySQL数据库
-- 2. 执行这个SQL脚本
-- 3. 启动Spring Boot应用，它会自动连接到这个数据库
-- ========================================== 