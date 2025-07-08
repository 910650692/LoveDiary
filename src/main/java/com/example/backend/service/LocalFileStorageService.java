package com.example.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 本地文件存储服务实现
 * 用于开发环境，文件存储在本地文件系统
 */
@Service
public class LocalFileStorageService implements FileStorageService {
    
    @Value("${app.file.upload.path:uploads}")
    private String uploadPath;
    
    @Value("${app.file.upload.url-prefix:/files}")
    private String urlPrefix;
    
    @Override
    public String upload(MultipartFile file, String path) {
        try {
            // 创建存储目录
            String fileName = generateFileName(file.getOriginalFilename());
            String relativePath = path + "/" + fileName;
            Path fullPath = Paths.get(uploadPath, relativePath);
            
            // 确保目录存在
            Files.createDirectories(fullPath.getParent());
            
            // 保存文件
            Files.copy(file.getInputStream(), fullPath);
            
            return relativePath;
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void delete(String filePath) {
        try {
            Path fullPath = Paths.get(uploadPath, filePath);
            if (Files.exists(fullPath)) {
                Files.delete(fullPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("文件删除失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String getFileUrl(String filePath) {
        return urlPrefix + "/" + filePath;
    }
    
    @Override
    public boolean exists(String filePath) {
        Path fullPath = Paths.get(uploadPath, filePath);
        return Files.exists(fullPath);
    }
    
    @Override
    public long getFileSize(String filePath) {
        try {
            Path fullPath = Paths.get(uploadPath, filePath);
            if (Files.exists(fullPath)) {
                return Files.size(fullPath);
            }
            return 0;
        } catch (IOException e) {
            return 0;
        }
    }
    
    /**
     * 生成唯一文件名
     */
    private String generateFileName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }
    
    /**
     * 检查是否为iOS实况图片格式
     */
    public boolean isLivePhoto(String fileName) {
        if (fileName == null) return false;
        String lowerFileName = fileName.toLowerCase();
        return lowerFileName.endsWith(".livp") || lowerFileName.endsWith(".live");
    }
    
    /**
     * 检查是否为HEIC格式
     */
    public boolean isHeicFormat(String fileName) {
        if (fileName == null) return false;
        String lowerFileName = fileName.toLowerCase();
        return lowerFileName.endsWith(".heic") || lowerFileName.endsWith(".heif");
    }
    
    /**
     * 获取文件的实际扩展名（处理iOS特殊格式）
     */
    public String getActualFileExtension(String fileName) {
        if (fileName == null) return "";
        
        String lowerFileName = fileName.toLowerCase();
        
        // iOS实况图片
        if (lowerFileName.endsWith(".livp")) {
            return ".livp";
        }
        
        // HEIC格式
        if (lowerFileName.endsWith(".heic")) {
            return ".heic";
        }
        if (lowerFileName.endsWith(".heif")) {
            return ".heif";
        }
        
        // 其他格式
        if (lowerFileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf("."));
        }
        
        return "";
    }
} 