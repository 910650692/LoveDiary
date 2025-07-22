package com.example.backend.event;

/**
 * 恋爱日期设置事件
 */
public class LoveDateSetEvent {
    private final Long coupleId;
    private final Long userId;
    private final String loveStartDate;
    
    public LoveDateSetEvent(Long coupleId, Long userId, String loveStartDate) {
        this.coupleId = coupleId;
        this.userId = userId;
        this.loveStartDate = loveStartDate;
    }
    
    // Getters
    public Long getCoupleId() { return coupleId; }
    public Long getUserId() { return userId; }
    public String getLoveStartDate() { return loveStartDate; }
}