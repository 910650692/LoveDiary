package com.example.backend.event;

/**
 * 用户加入情侣事件
 */
public class UserJoinedEvent {
    private final Long coupleId;
    private final Long userId;
    
    public UserJoinedEvent(Long coupleId, Long userId) {
        this.coupleId = coupleId;
        this.userId = userId;
    }
    
    // Getters
    public Long getCoupleId() { return coupleId; }
    public Long getUserId() { return userId; }
}