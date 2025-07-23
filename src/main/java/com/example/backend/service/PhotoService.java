package com.example.backend.service;

import com.example.backend.model.Album;
import com.example.backend.model.Couple;
import com.example.backend.model.Photo;
import com.example.backend.respository.AlbumRepository;
import com.example.backend.respository.CoupleRepository;
import com.example.backend.respository.PhotoRepository;
import com.example.backend.util.IOSFormatUtil;
import com.example.backend.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 照片服务类（MVP版本）
 * 处理照片的业务逻辑
 * 
 * 关于location和tags的说明：
 * - location（拍摄地点）：建议前端从照片EXIF元数据中提取GPS信息，然后通过地理编码服务转换为地址
 * - tags（标签）：建议前端根据拍摄时间、地点、照片内容等自动生成标签
 * - 后端直接接收前端处理好的信息，不进行元数据解析
 */
@Service
@Transactional
public class PhotoService {
    
    @Autowired
    private PhotoRepository photoRepository;
    
    @Autowired
    private AlbumRepository albumRepository;
    
    @Autowired
    private CoupleRepository coupleRepository;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    @Autowired
    private TagService tagService;
    
    /**
     * 上传照片
     * 
     * @param coupleId 情侣ID
     * @param creatorId 创建者ID
     * @param albumId 相册ID（可选）
     * @param file 上传的文件
     * @param photoData 照片信息，包含：
     *                  - description: 照片描述（可选）
     *                  - location: 拍摄地点（可选，建议前端从EXIF或用户输入获取）
     *                  - tagIds: 标签ID列表（可选，使用已创建的标签）
     * 
     * 前端处理建议：
     * 1. 使用iOS的PHAsset获取照片的EXIF信息
     * 2. 从EXIF中提取GPS坐标，使用CoreLocation进行地理编码
     * 3. 根据拍摄时间、地点、照片内容生成智能标签
     * 4. 将处理好的信息传递给后端
     */
    public Map<String, Object> uploadPhoto(Long coupleId, Long creatorId, Long albumId, 
                                         MultipartFile file, Map<String, Object> photoData) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 验证情侣关系
            if (!validateCoupleAccess(coupleId, creatorId)) {
                result.put("success", false);
                result.put("message", "您不属于这个情侣关系");
                return result;
            }
            
            // 验证相册（如果指定了相册）
            if (albumId != null) {
                Optional<Album> albumOpt = albumRepository.findByIdAndIsDeletedFalse(albumId);
                if (!albumOpt.isPresent()) {
                    result.put("success", false);
                    result.put("message", "相册不存在");
                    return result;
                }
                
                Album album = albumOpt.get();
                if (!album.getCoupleId().equals(coupleId)) {
                    result.put("success", false);
                    result.put("message", "相册不属于此情侣");
                    return result;
                }
            }
            
            // 验证文件
            if (file == null || file.isEmpty()) {
                result.put("success", false);
                result.put("message", "请选择要上传的文件");
                return result;
            }
            
            // 检查文件类型
            String contentType = file.getContentType();
            String fileName = file.getOriginalFilename();
            Photo.FileType fileType = determineFileType(contentType, fileName);
            if (fileType == null) {
                result.put("success", false);
                result.put("message", "不支持的文件类型");
                return result;
            }
            
            // 上传文件
            String filePath = "photos/" + coupleId + "/" + (albumId != null ? albumId : "uncategorized");
            String storedPath = fileStorageService.upload(file, filePath);
            
            // 创建照片记录
            Photo photo = new Photo();
            photo.setCoupleId(coupleId);
            photo.setAlbumId(albumId);
            photo.setCreatorId(creatorId);
            photo.setFileName(storedPath);
            photo.setOriginalName(file.getOriginalFilename());
            photo.setFilePath(storedPath);
            photo.setFileSize(file.getSize());
            photo.setFileType(fileType);
            photo.setMimeType(contentType);
            
            // 设置其他字段（从前端传递的参数）
            if (photoData != null) {
                photo.setDescription((String) photoData.get("description"));
                photo.setLocation((String) photoData.get("location"));
            }
            
            Photo savedPhoto = photoRepository.save(photo);
            
            // 处理标签分配
            if (photoData != null && photoData.containsKey("tagIds")) {
                @SuppressWarnings("unchecked")
                List<Long> tagIds = (List<Long>) photoData.get("tagIds");
                if (tagIds != null && !tagIds.isEmpty()) {
                    tagService.assignTagsToPhoto(savedPhoto.getId(), creatorId, tagIds);
                }
            }
            
            result.put("success", true);
            result.put("message", "照片上传成功");
            result.put("photo", createPhotoInfo(savedPhoto));
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "上传失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取情侣的所有照片
     */
    public Map<String, Object> getPhotos(Long coupleId, Long userId, Long albumId, 
                                       Boolean isFavorite, Photo.FileType fileType) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!validateCoupleAccess(coupleId, userId)) {
                result.put("success", false);
                result.put("message", "无权访问此情侣的照片");
                return result;
            }
            
            List<Photo> photos;
            
            if (albumId != null) {
                // 获取指定相册的照片
                photos = photoRepository.findByCoupleIdAndAlbumIdAndIsDeletedFalseOrderByCreatedAtDesc(coupleId, albumId);
            } else if (isFavorite != null && isFavorite) {
                // 获取收藏的照片
                photos = photoRepository.findByCoupleIdAndIsFavoriteTrueAndIsDeletedFalseOrderByCreatedAtDesc(coupleId);
            } else if (fileType != null) {
                // 按文件类型获取照片
                photos = photoRepository.findByCoupleIdAndFileTypeAndIsDeletedFalseOrderByCreatedAtDesc(coupleId, fileType);
            } else {
                // 获取所有照片
                photos = photoRepository.findByCoupleIdAndIsDeletedFalseOrderByCreatedAtDesc(coupleId);
            }
            
            List<Map<String, Object>> photoList = photos.stream()
                    .map(this::createPhotoInfo)
                    .collect(Collectors.toList());
            
            result.put("success", true);
            result.put("photos", photoList);
            result.put("count", photoList.size());
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取照片详情
     */
    public Map<String, Object> getPhoto(Long photoId, Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<Photo> photoOpt = photoRepository.findByIdAndIsDeletedFalse(photoId);
            if (!photoOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "照片不存在");
                return result;
            }
            
            Photo photo = photoOpt.get();
            
            if (!validateCoupleAccess(photo.getCoupleId(), userId)) {
                result.put("success", false);
                result.put("message", "无权访问此照片");
                return result;
            }
            
            result.put("success", true);
            result.put("photo", createPhotoInfo(photo));
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 更新照片信息
     */
    public Map<String, Object> updatePhoto(Long photoId, Long userId, Map<String, Object> updateData) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<Photo> photoOpt = photoRepository.findByIdAndIsDeletedFalse(photoId);
            if (!photoOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "照片不存在");
                return result;
            }
            
            Photo photo = photoOpt.get();
            
            if (!validateCoupleAccess(photo.getCoupleId(), userId)) {
                result.put("success", false);
                result.put("message", "无权修改此照片");
                return result;
            }
            
            // 更新字段
            if (updateData.containsKey("description")) {
                photo.setDescription((String) updateData.get("description"));
            }
            
            if (updateData.containsKey("location")) {
                photo.setLocation((String) updateData.get("location"));
            }
            
            if (updateData.containsKey("isFavorite")) {
                photo.setIsFavorite((Boolean) updateData.get("isFavorite"));
            }
            
            Photo updatedPhoto = photoRepository.save(photo);
            
            result.put("success", true);
            result.put("message", "照片更新成功");
            result.put("photo", createPhotoInfo(updatedPhoto));
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "更新失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 删除照片
     */
    public Map<String, Object> deletePhoto(Long photoId, Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<Photo> photoOpt = photoRepository.findByIdAndIsDeletedFalse(photoId);
            if (!photoOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "照片不存在");
                return result;
            }
            
            Photo photo = photoOpt.get();
            
            if (!validateCoupleAccess(photo.getCoupleId(), userId)) {
                result.put("success", false);
                result.put("message", "无权删除此照片");
                return result;
            }
            
            // 删除物理文件
            fileStorageService.delete(photo.getFilePath());
            
            // 软删除数据库记录
            photo.setIsDeleted(true);
            photoRepository.save(photo);
            
            result.put("success", true);
            result.put("message", "照片删除成功");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "删除失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 搜索照片
     */
    public Map<String, Object> searchPhotos(Long coupleId, Long userId, String keyword) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!validateCoupleAccess(coupleId, userId)) {
                result.put("success", false);
                result.put("message", "无权访问此情侣的照片");
                return result;
            }
            
            List<Photo> photos = photoRepository.searchByKeyword(coupleId, keyword);
            List<Map<String, Object>> photoList = photos.stream()
                    .map(this::createPhotoInfo)
                    .collect(Collectors.toList());
            
            result.put("success", true);
            result.put("photos", photoList);
            result.put("count", photoList.size());
            result.put("keyword", keyword);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "搜索失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 切换收藏状态
     */
    public Map<String, Object> toggleFavorite(Long photoId, Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<Photo> photoOpt = photoRepository.findByIdAndIsDeletedFalse(photoId);
            if (!photoOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "照片不存在");
                return result;
            }
            
            Photo photo = photoOpt.get();
            
            if (!validateCoupleAccess(photo.getCoupleId(), userId)) {
                result.put("success", false);
                result.put("message", "无权操作此照片");
                return result;
            }
            
            // 切换收藏状态
            photo.setIsFavorite(!photo.getIsFavorite());
            Photo updatedPhoto = photoRepository.save(photo);
            
            String message = photo.getIsFavorite() ? "已添加到收藏" : "已取消收藏";
            
            result.put("success", true);
            result.put("message", message);
//            result.put("photo", createPhotoInfo(updatedPhoto));
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "操作失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 批量删除照片
     */
    public Map<String, Object> batchDeletePhotos(List<Long> photoIds, Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (photoIds == null || photoIds.isEmpty()) {
                result.put("success", false);
                result.put("message", "请选择要删除的照片");
                return result;
            }
            
            List<Map<String, Object>> successList = new ArrayList<>();
            List<Map<String, Object>> failureList = new ArrayList<>();
            
            for (Long photoId : photoIds) {
                try {
                    Optional<Photo> photoOpt = photoRepository.findByIdAndIsDeletedFalse(photoId);
                    if (!photoOpt.isPresent()) {
                        Map<String, Object> failure = new HashMap<>();
                        failure.put("photoId", photoId);
                        failure.put("reason", "照片不存在");
                        failureList.add(failure);
                        continue;
                    }
                    
                    Photo photo = photoOpt.get();
                    
                    if (!validateCoupleAccess(photo.getCoupleId(), userId)) {
                        Map<String, Object> failure = new HashMap<>();
                        failure.put("photoId", photoId);
                        failure.put("reason", "无权删除此照片");
                        failureList.add(failure);
                        continue;
                    }
                    
                    // 删除物理文件
                    fileStorageService.delete(photo.getFilePath());
                    
                    // 软删除数据库记录
                    photo.setIsDeleted(true);
                    photoRepository.save(photo);
                    
                    Map<String, Object> success = new HashMap<>();
                    success.put("photoId", photoId);
                    success.put("fileName", photo.getOriginalName());
                    successList.add(success);
                    
                } catch (Exception e) {
                    Map<String, Object> failure = new HashMap<>();
                    failure.put("photoId", photoId);
                    failure.put("reason", "删除失败：" + e.getMessage());
                    failureList.add(failure);
                }
            }
            
            result.put("success", true);
            result.put("message", String.format("批量删除完成，成功 %d 张，失败 %d 张", 
                    successList.size(), failureList.size()));
            result.put("successList", successList);
            result.put("failureList", failureList);
            result.put("successCount", successList.size());
            result.put("failureCount", failureList.size());
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "批量删除失败：" + e.getMessage());
        }
        
        return result;
    }
    
    // ========== 私有辅助方法 ==========
    
    /**
     * 验证用户是否有权限访问情侣的照片
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
     * 根据MIME类型和文件名确定文件类型
     */
    private Photo.FileType determineFileType(String contentType, String fileName) {
        if (contentType == null && fileName == null) {
            return null;
        }
        
        // 检查iOS实况图片（LIVP格式）
        if (fileName != null && IOSFormatUtil.isLivePhoto(fileName)) {
            return Photo.FileType.PHOTO; // 实况图片归类为照片
        }
        
        // 检查HEIC格式
        if (fileName != null && IOSFormatUtil.isHeicFormat(fileName)) {
            return Photo.FileType.PHOTO;
        }
        
        // 根据MIME类型判断
        if (contentType != null) {
            if (contentType.startsWith("image/")) {
                return Photo.FileType.PHOTO;
            } else if (contentType.startsWith("video/")) {
                return Photo.FileType.VIDEO;
            }
        }
        
        // 根据文件扩展名判断
        if (fileName != null) {
            if (IOSFormatUtil.isImageFormat(fileName)) {
                return Photo.FileType.PHOTO;
            } else if (IOSFormatUtil.isVideoFormat(fileName)) {
                return Photo.FileType.VIDEO;
            }
        }
        
        return null;
    }
    
    /**
     * 创建照片信息Map
     */
    private Map<String, Object> createPhotoInfo(Photo photo) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", photo.getId());
        info.put("coupleId", photo.getCoupleId());
        info.put("albumId", photo.getAlbumId());
        info.put("creatorId", photo.getCreatorId());
        info.put("fileName", photo.getFileName());
        info.put("originalName", photo.getOriginalName());
        info.put("filePath", photo.getFilePath());
        info.put("fileSize", photo.getFileSize());
        info.put("fileType", photo.getFileType());
        info.put("mimeType", photo.getMimeType());
        info.put("width", photo.getWidth());
        info.put("height", photo.getHeight());
        info.put("duration", photo.getDuration());
        info.put("description", photo.getDescription());
        info.put("location", photo.getLocation());
        info.put("isFavorite", photo.getIsFavorite());
        info.put("createdAt", photo.getCreatedAt());
        info.put("updatedAt", photo.getUpdatedAt());
        
        // 添加文件访问URL
        info.put("fileUrl", fileStorageService.getFileUrl(photo.getFilePath()));
        
        // 添加文件类型描述
        info.put("fileTypeDescription", IOSFormatUtil.getFileTypeDescription(photo.getOriginalName()));
        
        // 添加标签信息
        List<Long> tagIds = tagService.getPhotoTagIds(photo.getId());
        info.put("tagIds", tagIds);
        
        return info;
    }
} 