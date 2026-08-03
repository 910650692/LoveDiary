package com.example.backend.event;

/**
 * 恋爱里程碑事件
 */
public class LoveMilestoneEvent {
    private final Long coupleId;
    private final Long userId;
    private final int days;
    
    public LoveMilestoneEvent(Long coupleId, Long userId, int days) {
        this.coupleId = coupleId;
        this.userId = userId;
        this.days = days;
    }
    
    // Getters
    public Long getCoupleId() { return coupleId; }
    public Long getUserId() { return userId; }
    public int getDays() { return days; }
}