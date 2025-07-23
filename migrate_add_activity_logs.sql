-- 数据库迁移脚本：添加用户动态功能
-- 文件名: migrate_add_activity_logs.sql
-- 执行时间: 2024-07-22

-- ===== 步骤1: 备份相关数据 =====
-- 创建备份表（可选）
-- CREATE TABLE activity_logs_backup AS SELECT * FROM activity_logs WHERE 1=0;

-- ===== 步骤2: 创建用户动态表 =====
CREATE TABLE IF NOT EXISTS activity_logs (
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
    
    CONSTRAINT fk_activity_couple FOREIGN KEY (couple_id) REFERENCES couples(id) ON DELETE CASCADE,
    CONSTRAINT fk_activity_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户动态表';

-- ===== 步骤3: 添加约束 =====
-- 为动态类型添加检查约束
ALTER TABLE activity_logs ADD CONSTRAINT chk_activity_type 
CHECK (activity_type IN (
    'TODO_COMPLETED', 'TODO_CREATED', 
    'PHOTO_UPLOADED', 'PHOTO_FAVORITED', 
    'ANNIVERSARY_CREATED', 'ANNIVERSARY_REMINDER', 
    'LOVE_MILESTONE', 'PERIOD_RECORD', 
    'ALBUM_CREATED', 'USER_JOINED', 'LOVE_DATE_SET'
));

-- ===== 步骤4: 插入基础测试数据 =====
-- 注意：这里的数据仅用于测试，实际部署时可能需要根据现有数据生成
INSERT INTO activity_logs (couple_id, user_id, activity_type, title, description, reference_id, reference_type, icon, created_at) 
SELECT 
    c.id as couple_id,
    c.user1_id as user_id,
    'USER_JOINED' as activity_type,
    '成功配对，开始恋爱日记' as title,
    '欢迎加入恋爱日记，一起记录美好时光！' as description,
    NULL as reference_id,
    NULL as reference_type,
    '💑' as icon,
    c.created_at
FROM couples c 
WHERE c.status = 'ACTIVE' 
AND NOT EXISTS (
    SELECT 1 FROM activity_logs a 
    WHERE a.couple_id = c.id 
    AND a.activity_type = 'USER_JOINED' 
    AND a.user_id = c.user1_id
);

-- 为user2也添加加入动态
INSERT INTO activity_logs (couple_id, user_id, activity_type, title, description, reference_id, reference_type, icon, created_at) 
SELECT 
    c.id as couple_id,
    c.user2_id as user_id,
    'USER_JOINED' as activity_type,
    '成功配对，开始恋爱日记' as title,
    '欢迎加入恋爱日记，一起记录美好时光！' as description,
    NULL as reference_id,
    NULL as reference_type,
    '💑' as icon,
    c.created_at
FROM couples c 
WHERE c.status = 'ACTIVE' 
AND c.user2_id IS NOT NULL
AND NOT EXISTS (
    SELECT 1 FROM activity_logs a 
    WHERE a.couple_id = c.id 
    AND a.activity_type = 'USER_JOINED' 
    AND a.user_id = c.user2_id
);

-- ===== 步骤5: 基于现有数据生成动态记录 =====

-- 基于已完成的TodoItem生成动态
INSERT INTO activity_logs (couple_id, user_id, activity_type, title, description, reference_id, reference_type, icon, created_at)
SELECT 
    t.couple_id,
    COALESCE(t.completer_id, t.creator_id) as user_id,
    'TODO_COMPLETED' as activity_type,
    CONCAT('完成了愿望"', t.title, '"') as title,
    COALESCE(t.description, '又完成了一个心愿，真棒！') as description,
    t.id as reference_id,
    'TodoItem' as reference_type,
    '💕' as icon,
    COALESCE(t.completed_at, t.updated_at) as created_at
FROM todo_items t
WHERE t.status = 'COMPLETED'
AND t.is_deleted = FALSE
AND NOT EXISTS (
    SELECT 1 FROM activity_logs a 
    WHERE a.reference_id = t.id 
    AND a.reference_type = 'TodoItem' 
    AND a.activity_type = 'TODO_COMPLETED'
);

-- 基于相册创建生成动态
INSERT INTO activity_logs (couple_id, user_id, activity_type, title, description, reference_id, reference_type, icon, created_at)
SELECT 
    a.couple_id,
    a.creator_id as user_id,
    'ALBUM_CREATED' as activity_type,
    CONCAT('创建了相册"', a.name, '"') as title,
    '整理照片，记录美好' as description,
    a.id as reference_id,
    'Album' as reference_type,
    '📁' as icon,
    a.created_at
FROM albums a
WHERE a.is_deleted = FALSE
AND NOT EXISTS (
    SELECT 1 FROM activity_logs al 
    WHERE al.reference_id = a.id 
    AND al.reference_type = 'Album' 
    AND al.activity_type = 'ALBUM_CREATED'
);

-- 基于照片收藏生成动态
INSERT INTO activity_logs (couple_id, user_id, activity_type, title, description, reference_id, reference_type, icon, created_at)
SELECT 
    p.couple_id,
    p.uploader_id as user_id,
    'PHOTO_FAVORITED' as activity_type,
    '收藏了一张照片' as title,
    '这张照片太美了，值得收藏' as description,
    p.id as reference_id,
    'Photo' as reference_type,
    '💖' as icon,
    p.updated_at
FROM photos p
WHERE p.is_favorited = TRUE
AND p.is_deleted = FALSE
AND NOT EXISTS (
    SELECT 1 FROM activity_logs al 
    WHERE al.reference_id = p.id 
    AND al.reference_type = 'Photo' 
    AND al.activity_type = 'PHOTO_FAVORITED'
);

-- 基于纪念日创建生成动态
INSERT INTO activity_logs (couple_id, user_id, activity_type, title, description, reference_id, reference_type, icon, created_at)
SELECT 
    ann.couple_id,
    ann.creator_id as user_id,
    'ANNIVERSARY_CREATED' as activity_type,
    CONCAT('创建了纪念日"', ann.title, '"') as title,
    '又多了一个值得纪念的日子' as description,
    ann.id as reference_id,
    'Anniversary' as reference_type,
    '🎂' as icon,
    ann.created_at
FROM anniversaries ann
WHERE ann.is_deleted = FALSE
AND NOT EXISTS (
    SELECT 1 FROM activity_logs al 
    WHERE al.reference_id = ann.id 
    AND al.reference_type = 'Anniversary' 
    AND al.activity_type = 'ANNIVERSARY_CREATED'
);

-- ===== 步骤6: 创建清理旧数据的存储过程 =====
DELIMITER //

CREATE PROCEDURE CleanOldActivities(IN days_to_keep INT)
BEGIN
    DECLARE cutoff_date TIMESTAMP DEFAULT DATE_SUB(NOW(), INTERVAL days_to_keep DAY);
    
    -- 软删除超过指定天数的动态
    UPDATE activity_logs 
    SET is_deleted = TRUE, deleted_at = NOW()
    WHERE created_at < cutoff_date 
    AND is_deleted = FALSE;
    
    -- 返回删除的记录数
    SELECT ROW_COUNT() as deleted_count;
END //

DELIMITER ;

-- ===== 步骤7: 验证数据完整性 =====
-- 检查数据是否正确插入
SELECT 
    activity_type,
    COUNT(*) as count,
    MIN(created_at) as earliest,
    MAX(created_at) as latest
FROM activity_logs 
WHERE is_deleted = FALSE
GROUP BY activity_type
ORDER BY count DESC;

-- 检查引用完整性
SELECT 
    al.reference_type,
    COUNT(*) as total_references,
    COUNT(CASE 
        WHEN al.reference_type = 'TodoItem' AND t.id IS NULL THEN 1
        WHEN al.reference_type = 'Album' AND a.id IS NULL THEN 1
        WHEN al.reference_type = 'Photo' AND p.id IS NULL THEN 1
        WHEN al.reference_type = 'Anniversary' AND ann.id IS NULL THEN 1
        ELSE NULL
    END) as orphaned_references
FROM activity_logs al
LEFT JOIN todo_items t ON al.reference_type = 'TodoItem' AND al.reference_id = t.id
LEFT JOIN albums a ON al.reference_type = 'Album' AND al.reference_id = a.id  
LEFT JOIN photos p ON al.reference_type = 'Photo' AND al.reference_id = p.id
LEFT JOIN anniversaries ann ON al.reference_type = 'Anniversary' AND al.reference_id = ann.id
WHERE al.is_deleted = FALSE
AND al.reference_id IS NOT NULL
GROUP BY al.reference_type;

-- ===== 回滚计划 =====
-- 如果需要回滚，执行以下命令：
-- DROP TABLE activity_logs;
-- DROP PROCEDURE CleanOldActivities;

-- ===== 完成 =====
SELECT 'Activity logs migration completed successfully!' as status;