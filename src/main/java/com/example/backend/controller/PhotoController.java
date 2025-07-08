package com.example.backend.controller;

import com.example.backend.model.Photo;
import com.example.backend.service.PhotoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 照片控制器（MVP版本）
 * 提供照片相关的API接口
 */
@RestController
@RequestMapping("/api/photos")
@CrossOrigin(origins = "*")
public class PhotoController {
    
    @Autowired
    private PhotoService photoService;
    
    /**
     * 搜索照片
     * GET /api/photos/search?coupleId={coupleId}&userId={userId}&keyword={keyword}
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchPhotos(
            @RequestParam Long coupleId,
            @RequestParam Long userId,
            @RequestParam String keyword) {
        
        Map<String, Object> result = photoService.searchPhotos(coupleId, userId, keyword);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取收藏的照片
     * GET /api/photos/favorites?coupleId={coupleId}&userId={userId}
     */
    @GetMapping("/favorites")
    public ResponseEntity<Map<String, Object>> getFavoritePhotos(
            @RequestParam Long coupleId,
            @RequestParam Long userId) {
        
        Map<String, Object> result = photoService.getPhotos(coupleId, userId, null, true, null);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取指定相册的照片
     * GET /api/photos/album/{albumId}?coupleId={coupleId}&userId={userId}
     */
    @GetMapping("/album/{albumId}")
    public ResponseEntity<Map<String, Object>> getAlbumPhotos(
            @PathVariable Long albumId,
            @RequestParam Long coupleId,
            @RequestParam Long userId) {
        
        Map<String, Object> result = photoService.getPhotos(coupleId, userId, albumId, null, null);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取情侣的所有照片
     * GET /api/photos?coupleId={coupleId}&userId={userId}&albumId={albumId}&isFavorite={isFavorite}&fileType={fileType}
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getPhotos(
            @RequestParam Long coupleId,
            @RequestParam Long userId,
            @RequestParam(required = false) Long albumId,
            @RequestParam(required = false) Boolean isFavorite,
            @RequestParam(required = false) Photo.FileType fileType) {
        
        Map<String, Object> result = photoService.getPhotos(coupleId, userId, albumId, isFavorite, fileType);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取照片详情
     * GET /api/photos/{photoId}?userId={userId}
     */
    @GetMapping("/{photoId}")
    public ResponseEntity<Map<String, Object>> getPhoto(
            @PathVariable Long photoId,
            @RequestParam Long userId) {
        
        Map<String, Object> result = photoService.getPhoto(photoId, userId);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 更新照片信息
     * PUT /api/photos/{photoId}?userId={userId}
     */
    @PutMapping("/{photoId}")
    public ResponseEntity<Map<String, Object>> updatePhoto(
            @PathVariable Long photoId,
            @RequestParam Long userId,
            @RequestBody Map<String, Object> updateData) {
        
        Map<String, Object> result = photoService.updatePhoto(photoId, userId, updateData);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 切换收藏状态
     * POST /api/photos/{photoId}/favorite?userId={userId}
     */
    @PostMapping("/{photoId}/favorite")
    public ResponseEntity<Map<String, Object>> toggleFavorite(
            @PathVariable Long photoId,
            @RequestParam Long userId) {
        
        Map<String, Object> result = photoService.toggleFavorite(photoId, userId);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 删除照片
     * DELETE /api/photos/{photoId}?userId={userId}
     */
    @DeleteMapping("/{photoId}")
    public ResponseEntity<Map<String, Object>> deletePhoto(
            @PathVariable Long photoId,
            @RequestParam Long userId) {
        
        Map<String, Object> result = photoService.deletePhoto(photoId, userId);
        return ResponseEntity.ok(result);
    }
} 