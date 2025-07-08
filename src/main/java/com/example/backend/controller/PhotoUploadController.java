package com.example.backend.controller;

import com.example.backend.service.PhotoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 照片上传控制器
 * 专门处理照片上传功能，避免路径冲突
 */
@RestController
@RequestMapping("/api/photo-upload")
@CrossOrigin(origins = "*")
public class PhotoUploadController {
    
    @Autowired
    private PhotoService photoService;
    
    /**
     * 上传照片
     * POST /api/photo-upload
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> uploadPhoto(
            @RequestParam Long coupleId,
            @RequestParam Long creatorId,
            @RequestParam(required = false) Long albumId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String tags) {
        
        Map<String, Object> photoData = Map.of(
            "description", description != null ? description : "",
            "location", location != null ? location : "",
            "tags", tags != null ? tags : ""
        );
        
        Map<String, Object> result = photoService.uploadPhoto(coupleId, creatorId, albumId, file, photoData);
        return ResponseEntity.ok(result);
    }
} 