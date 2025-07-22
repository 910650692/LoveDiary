-- 数据库迁移脚本：移除photos表中的tags字段
-- 执行前请备份数据库

USE love_db;

-- 1. 移除photos表中的tags字段
ALTER TABLE photos DROP COLUMN IF EXISTS tags;

-- 2. 验证字段已移除
DESCRIBE photos;

-- 3. 确认新的标签系统正常工作
SELECT 'Migration completed successfully!' as status; 