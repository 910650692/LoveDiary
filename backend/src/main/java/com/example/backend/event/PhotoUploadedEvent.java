package com.example.backend.event;

/**
 * 照片上传事件
 */
public class PhotoUploadedEvent {
    private final Long coupleId;
    private final Long userId;
    private final Long photoId;
    private final int photoCount;
    
    public PhotoUploadedEvent(Long coupleId, Long userId, Long photoId, int photoCount) {
        this.coupleId = coupleId;
        this.userId = userId;
        this.photoId = photoId;
        this.photoCount = photoCount;
    }
    
    // Getters
    public Long getCoupleId() { return coupleId; }
    public Long getUserId() { return userId; }
    public Long getPhotoId() { return photoId; }
    public int getPhotoCount() { return photoCount; }
}