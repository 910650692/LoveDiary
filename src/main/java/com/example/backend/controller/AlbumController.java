package com.example.backend.controller;

import com.example.backend.service.AlbumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 相册控制器（MVP版本）
 * 提供相册相关的API接口
 */
@RestController
@RequestMapping("/api/albums")
@CrossOrigin(origins = "*")
public class AlbumController {
    
    @Autowired
    private AlbumService albumService;
    
    /**
     * 创建相册
     * POST /api/albums
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createAlbum(
            @RequestParam Long coupleId,
            @RequestParam Long creatorId,
            @RequestBody Map<String, Object> albumData) {
        
        Map<String, Object> result = albumService.createAlbum(coupleId, creatorId, albumData);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取情侣的所有相册
     * GET /api/albums?coupleId={coupleId}&userId={userId}
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAlbums(
            @RequestParam Long coupleId,
            @RequestParam Long userId) {
        
        Map<String, Object> result = albumService.getAlbums(coupleId, userId);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取相册详情
     * GET /api/albums/{albumId}?userId={userId}
     */
    @GetMapping("/{albumId}")
    public ResponseEntity<Map<String, Object>> getAlbum(
            @PathVariable Long albumId,
            @RequestParam Long userId) {
        
        Map<String, Object> result = albumService.getAlbum(albumId, userId);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 更新相册
     * PUT /api/albums/{albumId}?userId={userId}
     */
    @PutMapping("/{albumId}")
    public ResponseEntity<Map<String, Object>> updateAlbum(
            @PathVariable Long albumId,
            @RequestParam Long userId,
            @RequestBody Map<String, Object> updateData) {
        
        Map<String, Object> result = albumService.updateAlbum(albumId, userId, updateData);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 删除相册
     * DELETE /api/albums/{albumId}?userId={userId}
     */
    @DeleteMapping("/{albumId}")
    public ResponseEntity<Map<String, Object>> deleteAlbum(
            @PathVariable Long albumId,
            @RequestParam Long userId) {
        
        Map<String, Object> result = albumService.deleteAlbum(albumId, userId);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 搜索相册
     * GET /api/albums/search?coupleId={coupleId}&userId={userId}&keyword={keyword}
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchAlbums(
            @RequestParam Long coupleId,
            @RequestParam Long userId,
            @RequestParam String keyword) {
        
        Map<String, Object> result = albumService.searchAlbums(coupleId, userId, keyword);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取相册统计信息
     * GET /api/albums/stats?coupleId={coupleId}&userId={userId}
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAlbumStats(
            @RequestParam Long coupleId,
            @RequestParam Long userId) {
        
        Map<String, Object> result = albumService.getAlbumStats(coupleId, userId);
        return ResponseEntity.ok(result);
    }
} 