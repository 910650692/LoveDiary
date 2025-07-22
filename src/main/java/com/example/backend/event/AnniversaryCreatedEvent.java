package com.example.backend.event;

/**
 * 纪念日创建事件
 */
public class AnniversaryCreatedEvent {
    private final Long coupleId;
    private final Long userId;
    private final Long anniversaryId;
    private final String anniversaryTitle;
    
    public AnniversaryCreatedEvent(Long coupleId, Long userId, Long anniversaryId, String anniversaryTitle) {
        this.coupleId = coupleId;
        this.userId = userId;
        this.anniversaryId = anniversaryId;
        this.anniversaryTitle = anniversaryTitle;
    }
    
    // Getters
    public Long getCoupleId() { return coupleId; }
    public Long getUserId() { return userId; }
    public Long getAnniversaryId() { return anniversaryId; }
    public String getAnniversaryTitle() { return anniversaryTitle; }
}