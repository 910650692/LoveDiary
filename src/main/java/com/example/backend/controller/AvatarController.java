package com.example.backend.controller;

import com.example.backend.service.AvatarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * 头像上传控制器
 * 提供用户头像的上传、获取、删除等API接口
 */
@RestController
@RequestMapping("/api/users")
public class AvatarController {
    
    @Autowired
    private AvatarService avatarService;
    
    /**
     * 上传用户头像
     * POST /api/users/{userId}/avatar/upload
     * Content-Type: multipart/form-data
     * Body: file (图片文件)
     */
    @PostMapping("/{userId}/avatar/upload")
    public Map<String, Object> uploadAvatar(
            @PathVariable Long userId,
            @RequestParam(value = "file", required = false) MultipartFile file,
            HttpServletRequest request) {
        
        // 验证权限：只能修改自己的头像
        Long currentUserId = (Long) request.getAttribute("userId");
        if (!currentUserId.equals(userId)) {
            return Map.of(
                "success", false,
                "message", "只能修改自己的头像"
            );
        }
        
        // 检查文件是否为空
        if (file == null || file.isEmpty()) {
            return Map.of(
                "success", false,
                "message", "请选择要上传的文件"
            );
        }
        
        return avatarService.uploadAvatar(userId, file);
    }
    
    /**
     * 测试文件上传配置
     * POST /api/users/test-upload
     */
    @PostMapping("/test-upload")
    public Map<String, Object> testUpload(@RequestParam(value = "file", required = false) MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        
        if (file == null) {
            result.put("success", false);
            result.put("message", "没有接收到文件");
            return result;
        }
        
        if (file.isEmpty()) {
            result.put("success", false);
            result.put("message", "文件为空");
            return result;
        }
        
        result.put("success", true);
        result.put("message", "文件上传测试成功");
        result.put("filename", file.getOriginalFilename());
        result.put("size", file.getSize());
        result.put("contentType", file.getContentType());
        
        return result;
    }
    
    /**
     * 获取用户头像URL
     * GET /api/users/{userId}/avatar
     */
    @GetMapping("/{userId}/avatar")
    public Map<String, Object> getAvatarUrl(@PathVariable Long userId) {
        return avatarService.getAvatarUrl(userId);
    }
    
    /**
     * 删除用户头像（恢复默认头像）
     * DELETE /api/users/{userId}/avatar
     */
    @DeleteMapping("/{userId}/avatar")
    public Map<String, Object> deleteAvatar(
            @PathVariable Long userId,
            HttpServletRequest request) {
        
        // 验证权限：只能删除自己的头像
        Long currentUserId = (Long) request.getAttribute("userId");
        if (!currentUserId.equals(userId)) {
            return Map.of(
                "success", false,
                "message", "只能删除自己的头像"
            );
        }
        
        return avatarService.deleteAvatar(userId);
    }
} 