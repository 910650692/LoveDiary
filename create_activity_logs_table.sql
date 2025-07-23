-- 创建用户动态表
CREATE TABLE activity_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    couple_id BIGINT NOT NULL COMMENT '所属情侣ID',
    user_id BIGINT NOT NULL COMMENT '触发动态的用户ID',
    activity_type VARCHAR(50) NOT NULL COMMENT '动态类型',
    title VARCHAR(200) NOT NULL COMMENT '动态标题',
    description VARCHAR(500) COMMENT '动态描述',
    reference_id BIGINT COMMENT '关联的实体ID',
    reference_type VARCHAR(50) COMMENT '关联的实体类型',
    metadata TEXT COMMENT '额外的元数据（JSON格式）',
    icon VARCHAR(10) COMMENT '动态的图标或emoji',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '动态创建时间',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否已删除',
    deleted_at TIMESTAMP NULL COMMENT '删除时间',
    
    INDEX idx_couple_created (couple_id, created_at DESC),
    INDEX idx_couple_type (couple_id, activity_type),
    INDEX idx_couple_user (couple_id, user_id),
    INDEX idx_reference (reference_id, reference_type),
    INDEX idx_created_at (created_at),
    INDEX idx_is_deleted (is_deleted),
    
    FOREIGN KEY (couple_id) REFERENCES couples(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户动态表';

-- 为动态类型添加检查约束
ALTER TABLE activity_logs ADD CONSTRAINT chk_activity_type 
CHECK (activity_type IN (
    'TODO_COMPLETED', 'TODO_CREATED', 
    'PHOTO_UPLOADED', 'PHOTO_FAVORITED', 
    'ANNIVERSARY_CREATED', 'ANNIVERSARY_REMINDER', 
    'LOVE_MILESTONE', 'PERIOD_RECORD', 
    'ALBUM_CREATED', 'USER_JOINED', 'LOVE_DATE_SET'
));

-- 插入一些示例数据用于测试
INSERT INTO activity_logs (couple_id, user_id, activity_type, title, description, reference_id, reference_type, icon, created_at) VALUES
-- 假设couple_id=1, user_id=1的情侣的动态
(1, 1, 'TODO_COMPLETED', '完成了愿望"一起看电影"', '我们一起看了最新的爱情电影，度过了美好的夜晚', 1, 'TodoItem', '💕', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(1, 2, 'PHOTO_UPLOADED', '上传了3张新照片', '在海边拍的美美的照片', 5, 'Photo', '📸', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(1, 1, 'ANNIVERSARY_REMINDER', '距离生日还有7天', '小仙女的生日快到了，要准备惊喜哦', 2, 'Anniversary', '🎂', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(1, 2, 'LOVE_MILESTONE', '今天是我们恋爱第500天', '时间过得真快，感谢有你相伴', NULL, NULL, '🌟', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(1, 1, 'ALBUM_CREATED', '创建了相册"旅行记忆"', '把我们的旅行照片整理到了新相册', 3, 'Album', '📁', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(1, 2, 'TODO_CREATED', '添加了新愿望"学习做饭"', '想要一起学习做一道拿手菜', 3, 'TodoItem', '✨', DATE_SUB(NOW(), INTERVAL 1 WEEK)),
(1, 1, 'PERIOD_RECORD', '记录了生理期信息', '开始新的生理周期', 1, 'PeriodRecord', '🌙', DATE_SUB(NOW(), INTERVAL 2 WEEK)),
(1, 2, 'PHOTO_FAVORITED', '收藏了一张照片', '这张照片太美了，值得收藏', 8, 'Photo', '💖', DATE_SUB(NOW(), INTERVAL 2 WEEK));