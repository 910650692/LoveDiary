package com.example.backend.controller;

import com.example.backend.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * 文件访问控制器
 * 提供文件下载和访问的API接口
 */
@RestController
@RequestMapping("/files")
@CrossOrigin(origins = "*")
public class FileController {
    
    @Autowired
    private FileStorageService fileStorageService;
    
    /**
     * 下载文件
     * GET /files/{filePath}
     */
    @GetMapping("/{filePath:.*}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filePath) {
        try {
            // 构建文件路径
            Path path = Paths.get("uploads", filePath);
            Resource resource = new UrlResource(path.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                // 获取文件名
                String fileName = resource.getFilename();
                
                // 设置响应头
                HttpHeaders headers = new HttpHeaders();
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"");
                
                // 根据文件类型设置Content-Type
                String contentType = determineContentType(fileName);
                headers.setContentType(MediaType.parseMediaType(contentType));
                
                return ResponseEntity.ok()
                        .headers(headers)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 获取文件信息
     * GET /files/info/{filePath}
     */
    @GetMapping("/info/{filePath:.*}")
    public ResponseEntity<Object> getFileInfo(@PathVariable String filePath) {
        try {
            if (fileStorageService.exists(filePath)) {
                return ResponseEntity.ok(Map.of(
                    "exists", true,
                    "fileSize", fileStorageService.getFileSize(filePath),
                    "fileUrl", fileStorageService.getFileUrl(filePath)
                ));
            } else {
                return ResponseEntity.ok(Map.of("exists", false));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("exists", false, "error", e.getMessage()));
        }
    }
    
    /**
     * 根据文件名确定Content-Type
     */
    private String determineContentType(String fileName) {
        if (fileName == null) {
            return "application/octet-stream";
        }
        
        String extension = "";
        if (fileName.contains(".")) {
            extension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
        }
        
        return switch (extension) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png" -> "image/png";
            case ".gif" -> "image/gif";
            case ".bmp" -> "image/bmp";
            case ".webp" -> "image/webp";
            // iOS HEIC格式
            case ".heic" -> "image/heic";
            case ".heif" -> "image/heif";
            // iOS实况图片
            case ".livp" -> "application/octet-stream"; // LIVP是压缩包格式
            case ".live" -> "application/octet-stream";
            // 视频格式
            case ".mp4" -> "video/mp4";
            case ".avi" -> "video/x-msvideo";
            case ".mov" -> "video/quicktime";
            case ".wmv" -> "video/x-ms-wmv";
            case ".flv" -> "video/x-flv";
            case ".webm" -> "video/webm";
            // iOS视频格式
            case ".m4v" -> "video/x-m4v";
            case ".3gp" -> "video/3gpp";
            default -> "application/octet-stream";
        };
    }
} 