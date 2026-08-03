package com.example.backend.service;

import com.example.backend.model.Tag;
import com.example.backend.model.PhotoTag;
import com.example.backend.respository.TagRepository;
import com.example.backend.respository.CoupleRepository;
import com.example.backend.respository.PhotoTagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class TagService {
    
    @Autowired
    private TagRepository tagRepository;
    
    @Autowired
    private CoupleRepository coupleRepository;
    
    @Autowired
    private PhotoTagRepository photoTagRepository;
    
    /**
     * 创建标签
     */
    public Map<String, Object> createTag(Long coupleId, Long userId, String name, String color) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 验证情侣关系
            if (!validateCoupleAccess(coupleId, userId)) {
                result.put("success", false);
                result.put("message", "您不属于这个情侣关系");
                return result;
            }
            
            // 验证标签名
            if (name == null || name.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "标签名不能为空");
                return result;
            }
            
            name = name.trim();
            if (name.length() > 32) {
                result.put("success", false);
                result.put("message", "标签名不能超过32个字符");
                return result;
            }
            
            // 检查标签名是否已存在
            if (tagRepository.existsByCoupleIdAndNameAndNotDeleted(coupleId, name)) {
                result.put("success", false);
                result.put("message", "标签名已存在");
                return result;
            }
            
            // 验证颜色
            if (color == null || color.trim().isEmpty()) {
                color = "#FFB300"; // 默认颜色
            }
            
            // 创建标签
            Tag tag = new Tag();
            tag.setCoupleId(coupleId);
            tag.setName(name);
            tag.setColor(color);
            tag.setCreatedBy(userId);
            
            Tag savedTag = tagRepository.save(tag);
            
            result.put("success", true);
            result.put("message", "标签创建成功");
            result.put("tag", createTagInfo(savedTag));
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "创建失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取情侣的所有标签
     */
    public Map<String, Object> getTags(Long coupleId, Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!validateCoupleAccess(coupleId, userId)) {
                result.put("success", false);
                result.put("message", "无权访问此情侣的标签");
                return result;
            }
            
            List<Tag> tags = tagRepository.findByCoupleIdAndIsDeletedFalseOrderByCreatedAtDesc(coupleId);
            List<Map<String, Object>> tagList = tags.stream()
                    .map(this::createTagInfo)
                    .collect(java.util.stream.Collectors.toList());
            
            result.put("success", true);
            result.put("tags", tagList);
            result.put("count", tagList.size());
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 更新标签
     */
    public Map<String, Object> updateTag(Long tagId, Long userId, String name, String color) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<Tag> tagOpt = tagRepository.findByIdAndIsDeletedFalse(tagId);
            if (!tagOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "标签不存在");
                return result;
            }
            
            Tag tag = tagOpt.get();
            
            if (!validateCoupleAccess(tag.getCoupleId(), userId)) {
                result.put("success", false);
                result.put("message", "无权修改此标签");
                return result;
            }
            
            // 验证标签名
            if (name != null && !name.trim().isEmpty()) {
                name = name.trim();
                if (name.length() > 32) {
                    result.put("success", false);
                    result.put("message", "标签名不能超过32个字符");
                    return result;
                }
                
                // 检查新名称是否与其他标签重复
                if (!name.equals(tag.getName()) && 
                    tagRepository.existsByCoupleIdAndNameAndNotDeleted(tag.getCoupleId(), name)) {
                    result.put("success", false);
                    result.put("message", "标签名已存在");
                    return result;
                }
                
                tag.setName(name);
            }
            
            // 更新颜色
            if (color != null && !color.trim().isEmpty()) {
                tag.setColor(color.trim());
            }
            
            Tag updatedTag = tagRepository.save(tag);
            
            result.put("success", true);
            result.put("message", "标签更新成功");
            result.put("tag", createTagInfo(updatedTag));
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "更新失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 删除标签
     */
    public Map<String, Object> deleteTag(Long tagId, Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<Tag> tagOpt = tagRepository.findByIdAndIsDeletedFalse(tagId);
            if (!tagOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "标签不存在");
                return result;
            }
            
            Tag tag = tagOpt.get();
            
            if (!validateCoupleAccess(tag.getCoupleId(), userId)) {
                result.put("success", false);
                result.put("message", "无权删除此标签");
                return result;
            }
            
            // 软删除标签
            tag.setIsDeleted(true);
            tagRepository.save(tag);
            
            // 删除标签的所有关系
            photoTagRepository.deleteByTagId(tagId);
            
            result.put("success", true);
            result.put("message", "标签删除成功");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "删除失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 为照片分配标签
     */
    public Map<String, Object> assignTagsToPhoto(Long photoId, Long userId, List<Long> tagIds) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 这里需要验证照片权限，暂时简化处理
            // 实际应该通过PhotoService验证
            
            // 删除照片的现有标签关系
            photoTagRepository.deleteByPhotoId(photoId);
            
            // 添加新的标签关系
            if (tagIds != null && !tagIds.isEmpty()) {
                for (Long tagId : tagIds) {
                    // 验证标签是否存在且属于同一情侣
                    Optional<Tag> tagOpt = tagRepository.findByIdAndIsDeletedFalse(tagId);
                    if (tagOpt.isPresent()) {
                        PhotoTag photoTag = new PhotoTag();
                        photoTag.setPhotoId(photoId);
                        photoTag.setTagId(tagId);
                        photoTagRepository.save(photoTag);
                    }
                }
            }
            
            result.put("success", true);
            result.put("message", "标签分配成功");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "分配失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取照片的标签
     */
    public List<Long> getPhotoTagIds(Long photoId) {
        return photoTagRepository.findTagIdsByPhotoId(photoId);
    }
    
    /**
     * 获取标签下的照片ID列表
     */
    public List<Long> getPhotoIdsByTag(Long tagId) {
        return photoTagRepository.findPhotoIdsByTagId(tagId);
    }
    
    // ========== 私有辅助方法 ==========
    
    /**
     * 验证用户是否有权限访问情侣的标签
     */
    private boolean validateCoupleAccess(Long coupleId, Long userId) {
        return coupleRepository.findById(coupleId)
                .map(couple -> couple.containsUser(userId))
                .orElse(false);
    }
    
    /**
     * 创建标签信息Map
     */
    private Map<String, Object> createTagInfo(Tag tag) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", tag.getId());
        info.put("coupleId", tag.getCoupleId());
        info.put("name", tag.getName());
        info.put("color", tag.getColor());
        info.put("createdBy", tag.getCreatedBy());
        info.put("createdAt", tag.getCreatedAt());
        return info;
    }
} 