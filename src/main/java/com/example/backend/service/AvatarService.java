package com.example.backend.service;

import com.example.backend.model.User;
import com.example.backend.respository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 头像上传服务类
 * 处理用户头像的上传、保存、删除等业务逻辑
 */
@Service
public class AvatarService {
    
    @Autowired
    private UserRepository userRepository;
    
    // 头像存储目录
    private static final String AVATAR_UPLOAD_DIR = "src/main/resources/static/uploads/avatars/";
    
    // 默认头像路径
    private static final String DEFAULT_MALE_AVATAR = "/images/default-male.png";
    private static final String DEFAULT_FEMALE_AVATAR = "/images/default-female.png";
    
    // 时间戳格式
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    
    /**
     * 上传用户头像
     */
    public Map<String, Object> uploadAvatar(Long userId, MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 验证用户是否存在
            Optional<User> userOpt = userRepository.findById(userId);
            if (!userOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "用户不存在");
                return result;
            }
            
            User user = userOpt.get();
            
            // 验证文件是否为空
            if (file.isEmpty()) {
                result.put("success", false);
                result.put("message", "请选择要上传的文件");
                return result;
            }
            
            // 生成文件名：用户ID + 时间戳 + 原文件扩展名
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            String filename = "user" + userId + "_" + timestamp + fileExtension;
            
            // 确保上传目录存在
            createUploadDirectoryIfNotExists();
            
            // 保存文件到本地
            Path filePath = Paths.get(AVATAR_UPLOAD_DIR + filename);
            Files.copy(file.getInputStream(), filePath);
            
            // 生成访问URL
            String avatarUrl = "/uploads/avatars/" + filename;
            
            // 更新用户头像URL
            user.setAvatarUrl(avatarUrl);
            userRepository.save(user);
            
            result.put("success", true);
            result.put("message", "头像上传成功");
            result.put("avatarUrl", avatarUrl);
            result.put("filename", filename);
            
        } catch (IOException e) {
            result.put("success", false);
            result.put("message", "文件上传失败：" + e.getMessage());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "头像上传失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 删除用户头像（恢复默认头像）
     */
    public Map<String, Object> deleteAvatar(Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 验证用户是否存在
            Optional<User> userOpt = userRepository.findById(userId);
            if (!userOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "用户不存在");
                return result;
            }
            
            User user = userOpt.get();
            
            // 删除旧头像文件（如果存在且不是默认头像）
            String currentAvatarUrl = user.getAvatarUrl();
            if (currentAvatarUrl != null && !currentAvatarUrl.equals(DEFAULT_MALE_AVATAR) && !currentAvatarUrl.equals(DEFAULT_FEMALE_AVATAR)) {
                deleteAvatarFile(currentAvatarUrl);
            }
            
            // 设置默认头像
            String defaultAvatarUrl = getDefaultAvatarUrl(user.getGender());
            user.setAvatarUrl(defaultAvatarUrl);
            userRepository.save(user);
            
            result.put("success", true);
            result.put("message", "头像已恢复为默认头像");
            result.put("avatarUrl", defaultAvatarUrl);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "删除头像失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取用户头像URL
     */
    public Map<String, Object> getAvatarUrl(Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (!userOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "用户不存在");
                return result;
            }
            
            User user = userOpt.get();
            String avatarUrl = user.getAvatarUrl();
            
            // 如果用户没有头像，返回默认头像
            if (avatarUrl == null || avatarUrl.isEmpty()) {
                avatarUrl = getDefaultAvatarUrl(user.getGender());
            }
            
            result.put("success", true);
            result.put("avatarUrl", avatarUrl);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取头像失败：" + e.getMessage());
        }
        
        return result;
    }
    
    // ========== 私有辅助方法 ==========
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return ".jpg"; // 默认扩展名
        }
        return filename.substring(filename.lastIndexOf("."));
    }
    
    /**
     * 创建上传目录（如果不存在）
     */
    private void createUploadDirectoryIfNotExists() throws IOException {
        Path uploadPath = Paths.get(AVATAR_UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
    }
    
    /**
     * 删除头像文件
     */
    private void deleteAvatarFile(String avatarUrl) {
        try {
            if (avatarUrl != null && avatarUrl.startsWith("/uploads/avatars/")) {
                String filename = avatarUrl.substring("/uploads/avatars/".length());
                Path filePath = Paths.get(AVATAR_UPLOAD_DIR + filename);
                Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            // 删除失败不影响主流程，只记录日志
            System.err.println("删除头像文件失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据性别获取默认头像URL
     */
    private String getDefaultAvatarUrl(User.Gender gender) {
        if (gender == User.Gender.FEMALE) {
            return DEFAULT_FEMALE_AVATAR;
        } else {
            return DEFAULT_MALE_AVATAR;
        }
    }
} 