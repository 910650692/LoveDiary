package com.example.backend.service;

import com.example.backend.model.Album;
import com.example.backend.model.Couple;
import com.example.backend.model.Photo;
import com.example.backend.respository.AlbumRepository;
import com.example.backend.respository.CoupleRepository;
import com.example.backend.respository.PhotoRepository;
import com.example.backend.service.PhotoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 相册服务类（MVP版本）
 * 处理相册的业务逻辑
 */
@Service
@Transactional
public class AlbumService {
    
    @Autowired
    private AlbumRepository albumRepository;
    
    @Autowired
    private CoupleRepository coupleRepository;
    
    @Autowired
    private PhotoRepository photoRepository;
    
    @Autowired
    private PhotoService photoService;
    
    /**
     * 创建相册
     */
    public Map<String, Object> createAlbum(Long coupleId, Long creatorId, Map<String, Object> albumData) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 验证情侣关系
            Optional<Couple> coupleOpt = coupleRepository.findById(coupleId);
            if (!coupleOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "情侣关系不存在");
                return result;
            }
            
            Couple couple = coupleOpt.get();
            if (!couple.containsUser(creatorId)) {
                result.put("success", false);
                result.put("message", "您不属于这个情侣关系");
                return result;
            }
            
            // 创建相册
            Album album = new Album();
            album.setCoupleId(coupleId);
            album.setName((String) albumData.get("name"));
            album.setDescription((String) albumData.get("description"));
            
            Album savedAlbum = albumRepository.save(album);
            
            result.put("success", true);
            result.put("message", "相册创建成功");
            result.put("album", createAlbumInfo(savedAlbum));
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "创建失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取情侣的所有相册
     */
    public Map<String, Object> getAlbums(Long coupleId, Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 验证权限
            if (!validateCoupleAccess(coupleId, userId)) {
                result.put("success", false);
                result.put("message", "无权访问此情侣的相册");
                return result;
            }
            
            List<Album> albums = albumRepository.findByCoupleIdAndIsDeletedFalseOrderByCreatedAtDesc(coupleId);
            List<Map<String, Object>> albumList = albums.stream()
                    .map(this::createAlbumInfo)
                    .collect(Collectors.toList());
            
            result.put("success", true);
            result.put("albums", albumList);
            result.put("count", albumList.size());
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取相册详情
     */
    public Map<String, Object> getAlbum(Long albumId, Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<Album> albumOpt = albumRepository.findByIdAndIsDeletedFalse(albumId);
            if (!albumOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "相册不存在");
                return result;
            }
            
            Album album = albumOpt.get();
            
            // 验证权限
            if (!validateCoupleAccess(album.getCoupleId(), userId)) {
                result.put("success", false);
                result.put("message", "无权访问此相册");
                return result;
            }
            
            Map<String, Object> albumInfo = createAlbumInfo(album);
            
            // 获取相册中的照片数量
            long photoCount = photoRepository.countByAlbumIdAndIsDeletedFalse(albumId);
            albumInfo.put("photoCount", photoCount);
            
            result.put("success", true);
            result.put("album", albumInfo);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 更新相册
     */
    public Map<String, Object> updateAlbum(Long albumId, Long userId, Map<String, Object> updateData) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<Album> albumOpt = albumRepository.findByIdAndIsDeletedFalse(albumId);
            if (!albumOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "相册不存在");
                return result;
            }
            
            Album album = albumOpt.get();
            
            // 验证权限
            if (!validateCoupleAccess(album.getCoupleId(), userId)) {
                result.put("success", false);
                result.put("message", "无权修改此相册");
                return result;
            }
            
            // 更新字段
            if (updateData.containsKey("name")) {
                album.setName((String) updateData.get("name"));
            }
            
            if (updateData.containsKey("description")) {
                album.setDescription((String) updateData.get("description"));
            }
            
            Album updatedAlbum = albumRepository.save(album);
            
            result.put("success", true);
            result.put("message", "相册更新成功");
            result.put("album", createAlbumInfo(updatedAlbum));
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "更新失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 删除相册
     */
    public Map<String, Object> deleteAlbum(Long albumId, Long userId, boolean deletePhotos) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<Album> albumOpt = albumRepository.findByIdAndIsDeletedFalse(albumId);
            if (!albumOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "相册不存在");
                return result;
            }
            
            Album album = albumOpt.get();
            
            // 验证权限
            if (!validateCoupleAccess(album.getCoupleId(), userId)) {
                result.put("success", false);
                result.put("message", "无权删除此相册");
                return result;
            }
            
            // 获取相册中的照片
            List<Photo> photos = photoRepository.findByAlbumIdAndIsDeletedFalse(albumId);
            
            if (!deletePhotos && !photos.isEmpty()) {
                // 如果不删除照片且相册中有照片，则不允许删除相册
                result.put("success", false);
                result.put("message", "相册中还有照片，请先删除照片或选择级联删除");
                result.put("photoCount", photos.size());
                return result;
            }
            
            if (deletePhotos && !photos.isEmpty()) {
                // 批量删除相册中的所有照片
                List<Long> photoIds = photos.stream()
                        .map(Photo::getId)
                        .collect(Collectors.toList());
                
                Map<String, Object> batchDeleteResult = photoService.batchDeletePhotos(photoIds, userId);
                
                // 如果批量删除有失败的，记录失败信息但继续删除相册
                if (batchDeleteResult.containsKey("failureCount") && 
                    (Integer) batchDeleteResult.get("failureCount") > 0) {
                    result.put("photoDeleteWarning", "部分照片删除失败");
                    result.put("photoDeleteDetails", batchDeleteResult);
                }
            }
            
            // 删除相册
            album.setIsDeleted(true);
            albumRepository.save(album);
            
            String message = deletePhotos && !photos.isEmpty() 
                ? String.format("相册及其 %d 张照片删除成功", photos.size())
                : "相册删除成功";
            
            result.put("success", true);
            result.put("message", message);
            result.put("deletedPhotoCount", deletePhotos ? photos.size() : 0);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "删除失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 搜索相册
     */
    public Map<String, Object> searchAlbums(Long coupleId, Long userId, String keyword) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!validateCoupleAccess(coupleId, userId)) {
                result.put("success", false);
                result.put("message", "无权访问此情侣的相册");
                return result;
            }
            
            List<Album> albums = albumRepository.searchByKeyword(coupleId, keyword);
            List<Map<String, Object>> albumList = albums.stream()
                    .map(this::createAlbumInfo)
                    .collect(Collectors.toList());
            
            result.put("success", true);
            result.put("albums", albumList);
            result.put("count", albumList.size());
            result.put("keyword", keyword);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "搜索失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取相册统计信息
     */
    public Map<String, Object> getAlbumStats(Long coupleId, Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!validateCoupleAccess(coupleId, userId)) {
                result.put("success", false);
                result.put("message", "无权访问此情侣的相册");
                return result;
            }
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalAlbums", albumRepository.countByCoupleIdAndIsDeletedFalse(coupleId));
            stats.put("totalPhotos", photoRepository.countByCoupleIdAndIsDeletedFalse(coupleId));
            stats.put("favoritePhotos", photoRepository.countByCoupleIdAndIsFavoriteTrueAndIsDeletedFalse(coupleId));
            stats.put("photoPhotos", photoRepository.countByCoupleIdAndFileTypeAndIsDeletedFalse(coupleId, com.example.backend.model.Photo.FileType.PHOTO));
            stats.put("videoPhotos", photoRepository.countByCoupleIdAndFileTypeAndIsDeletedFalse(coupleId, com.example.backend.model.Photo.FileType.VIDEO));
            
            result.put("success", true);
            result.put("stats", stats);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取统计信息失败：" + e.getMessage());
        }
        
        return result;
    }
    
    // ========== 私有辅助方法 ==========
    
    /**
     * 验证用户是否有权限访问情侣的相册
     */
    private boolean validateCoupleAccess(Long coupleId, Long userId) {
        Optional<Couple> coupleOpt = coupleRepository.findById(coupleId);
        if (!coupleOpt.isPresent()) {
            return false;
        }
        
        Couple couple = coupleOpt.get();
        return couple.containsUser(userId);
    }
    
    /**
     * 创建相册信息Map
     */
    private Map<String, Object> createAlbumInfo(Album album) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", album.getId());
        info.put("coupleId", album.getCoupleId());
        info.put("name", album.getName());
        info.put("description", album.getDescription());
        info.put("coverPhotoId", album.getCoverPhotoId());
        info.put("createdAt", album.getCreatedAt());
        info.put("updatedAt", album.getUpdatedAt());
        
        return info;
    }
} 