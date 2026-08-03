package com.example.backend.event;

/**
 * 相册创建事件
 */
public class AlbumCreatedEvent {
    private final Long coupleId;
    private final Long userId;
    private final Long albumId;
    private final String albumName;
    
    public AlbumCreatedEvent(Long coupleId, Long userId, Long albumId, String albumName) {
        this.coupleId = coupleId;
        this.userId = userId;
        this.albumId = albumId;
        this.albumName = albumName;
    }
    
    // Getters
    public Long getCoupleId() { return coupleId; }
    public Long getUserId() { return userId; }
    public Long getAlbumId() { return albumId; }
    public String getAlbumName() { return albumName; }
}