package com.example.backend.controller;

import com.example.backend.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tags")
public class TagController {
    
    @Autowired
    private TagService tagService;
    
    /**
     * 创建标签
     */
    @PostMapping
    public Map<String, Object> createTag(
            @RequestParam Long coupleId,
            @RequestParam Long userId,
            @RequestParam String name,
            @RequestParam(required = false) String color) {
        return tagService.createTag(coupleId, userId, name, color);
    }
    
    /**
     * 获取情侣的所有标签
     */
    @GetMapping
    public Map<String, Object> getTags(
            @RequestParam Long coupleId,
            @RequestParam Long userId) {
        return tagService.getTags(coupleId, userId);
    }
    
    /**
     * 更新标签
     */
    @PutMapping("/{tagId}")
    public Map<String, Object> updateTag(
            @PathVariable Long tagId,
            @RequestParam Long userId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String color) {
        return tagService.updateTag(tagId, userId, name, color);
    }
    
    /**
     * 删除标签
     */
    @DeleteMapping("/{tagId}")
    public Map<String, Object> deleteTag(
            @PathVariable Long tagId,
            @RequestParam Long userId) {
        return tagService.deleteTag(tagId, userId);
    }
    
    /**
     * 为照片分配标签
     */
    @PostMapping("/assign")
    public Map<String, Object> assignTagsToPhoto(
            @RequestParam Long photoId,
            @RequestParam Long userId,
            @RequestBody List<Long> tagIds) {
        return tagService.assignTagsToPhoto(photoId, userId, tagIds);
    }
} 