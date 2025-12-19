-- 情绪记录表
CREATE TABLE mood_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    couple_id BIGINT NOT NULL COMMENT '情侣ID',

    -- 情绪相关
    mood_type VARCHAR(20) NOT NULL COMMENT '心情类型: HAPPY, SAD, ANGRY, LOVE, MISS, NEUTRAL, ANXIOUS, EXCITED',
    mood_level INT DEFAULT 3 COMMENT '心情程度 1-5（1最差，5最好）',

    -- 记录内容
    reason TEXT COMMENT '心情原因/备注',
    record_date DATE NOT NULL COMMENT '记录日期',

    -- 时间戳
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,

    -- 索引
    UNIQUE KEY uk_user_date (user_id, record_date, is_deleted),
    INDEX idx_couple_date (couple_id, record_date),
    INDEX idx_user_id (user_id),

    -- 外键
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (couple_id) REFERENCES couples(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='情绪记录表';
