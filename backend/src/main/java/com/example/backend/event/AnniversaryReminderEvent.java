package com.example.backend.event;

/**
 * 纪念日提醒事件
 */
public class AnniversaryReminderEvent {
    private final Long coupleId;
    private final Long userId;
    private final Long anniversaryId;
    private final String anniversaryTitle;
    private final int daysLeft;
    
    public AnniversaryReminderEvent(Long coupleId, Long userId, Long anniversaryId, String anniversaryTitle, int daysLeft) {
        this.coupleId = coupleId;
        this.userId = userId;
        this.anniversaryId = anniversaryId;
        this.anniversaryTitle = anniversaryTitle;
        this.daysLeft = daysLeft;
    }
    
    // Getters
    public Long getCoupleId() { return coupleId; }
    public Long getUserId() { return userId; }
    public Long getAnniversaryId() { return anniversaryId; }
    public String getAnniversaryTitle() { return anniversaryTitle; }
    public int getDaysLeft() { return daysLeft; }
}