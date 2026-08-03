package com.example.backend.event;

/**
 * 生理期记录事件
 */
public class PeriodRecordEvent {
    private final Long coupleId;
    private final Long userId;
    private final Long periodRecordId;
    
    public PeriodRecordEvent(Long coupleId, Long userId, Long periodRecordId) {
        this.coupleId = coupleId;
        this.userId = userId;
        this.periodRecordId = periodRecordId;
    }
    
    // Getters
    public Long getCoupleId() { return coupleId; }
    public Long getUserId() { return userId; }
    public Long getPeriodRecordId() { return periodRecordId; }
}