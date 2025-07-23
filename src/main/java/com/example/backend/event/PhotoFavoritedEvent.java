package com.example.backend.event;

/**
 * 照片收藏事件
 */
public class PhotoFavoritedEvent {
    private final Long coupleId;
    private final Long userId;
    private final Long photoId;
    
    public PhotoFavoritedEvent(Long coupleId, Long userId, Long photoId) {
        this.coupleId = coupleId;
        this.userId = userId;
        this.photoId = photoId;
    }
    
    // Getters
    public Long getCoupleId() { return coupleId; }
    public Long getUserId() { return userId; }
    public Long getPhotoId() { return photoId; }
}