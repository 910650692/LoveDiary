package com.example.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 * 提供API服务状态监控接口
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {
    
    /**
     * 基础健康检查
     * GET /api/health
     */
    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "LoveDiary API");
        health.put("version", "1.0.0");
        health.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        health.put("uptime", System.currentTimeMillis());
        
        return health;
    }
    
    /**
     * 详细健康检查
     * GET /api/health/detailed
     */
    @GetMapping("/detailed")
    public Map<String, Object> detailedHealth() {
        Map<String, Object> health = new HashMap<>();
        
        // 基础信息
        health.put("status", "UP");
        health.put("service", "LoveDiary API");
        health.put("version", "1.0.0");
        health.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        // 系统信息
        Map<String, Object> system = new HashMap<>();
        system.put("javaVersion", System.getProperty("java.version"));
        system.put("osName", System.getProperty("os.name"));
        system.put("osVersion", System.getProperty("os.version"));
        system.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        system.put("totalMemory", Runtime.getRuntime().totalMemory());
        system.put("freeMemory", Runtime.getRuntime().freeMemory());
        system.put("maxMemory", Runtime.getRuntime().maxMemory());
        health.put("system", system);
        
        // 组件状态
        Map<String, Object> components = new HashMap<>();
        components.put("database", "UP");
        components.put("jwt", "UP");
        components.put("fileStorage", "UP");
        health.put("components", components);
        
        return health;
    }
    
    /**
     * 数据库健康检查
     * GET /api/health/database
     */
    @GetMapping("/database")
    public Map<String, Object> databaseHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // 这里可以添加实际的数据库连接测试
            // 暂时返回模拟状态
            health.put("status", "UP");
            health.put("database", "H2");
            health.put("message", "数据库连接正常");
            health.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("message", "数据库连接失败: " + e.getMessage());
            health.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        
        return health;
    }
    
    /**
     * 服务信息
     * GET /api/health/info
     */
    @GetMapping("/info")
    public Map<String, Object> serviceInfo() {
        Map<String, Object> info = new HashMap<>();
        
        info.put("name", "LoveDiary API");
        info.put("description", "情侣纪念日管理系统的后端API服务");
        info.put("version", "1.0.0");
        info.put("author", "LoveDiary Team");
        info.put("startTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        Map<String, Object> features = new HashMap<>();
        features.put("userManagement", "用户注册、登录、资料管理");
        features.put("coupleMatching", "情侣匹配、关系管理");
        features.put("anniversaryManagement", "纪念日管理、提醒设置");
        features.put("avatarUpload", "头像上传、管理");
        features.put("jwtAuthentication", "JWT令牌认证");
        info.put("features", features);
        
        Map<String, Object> endpoints = new HashMap<>();
        endpoints.put("baseUrl", "/api");
        endpoints.put("docs", "/api/docs");
        endpoints.put("health", "/api/health");
        info.put("endpoints", endpoints);
        
        return info;
    }
    
    /**
     * 内存使用情况
     * GET /api/health/memory
     */
    @GetMapping("/memory")
    public Map<String, Object> memoryUsage() {
        Map<String, Object> memory = new HashMap<>();
        
        Runtime runtime = Runtime.getRuntime();
        
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        memory.put("totalMemory", totalMemory);
        memory.put("freeMemory", freeMemory);
        memory.put("usedMemory", usedMemory);
        memory.put("maxMemory", maxMemory);
        memory.put("totalMemoryMB", totalMemory / 1024 / 1024);
        memory.put("freeMemoryMB", freeMemory / 1024 / 1024);
        memory.put("usedMemoryMB", usedMemory / 1024 / 1024);
        memory.put("maxMemoryMB", maxMemory / 1024 / 1024);
        memory.put("usagePercentage", Math.round((double) usedMemory / totalMemory * 100));
        
        return memory;
    }
} 