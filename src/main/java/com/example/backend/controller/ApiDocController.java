package com.example.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * API文档控制器
 * 提供API接口的说明和使用指南
 */
@RestController
@RequestMapping("/api/docs")
public class ApiDocController {
    
    /**
     * 获取API概览
     * GET /api/docs
     */
    @GetMapping
    public Map<String, Object> getApiOverview() {
        Map<String, Object> overview = new HashMap<>();
        overview.put("title", "LoveDiary API 接口文档");
        overview.put("version", "1.0.0");
        overview.put("description", "情侣纪念日管理系统的后端API接口");
        overview.put("baseUrl", "/api");
        
        Map<String, Object> endpoints = new HashMap<>();
        
        // 用户相关接口
        Map<String, Object> userEndpoints = new HashMap<>();
        userEndpoints.put("register", "POST /api/users/register - 用户注册");
        userEndpoints.put("login", "POST /api/users/login - 用户登录");
        userEndpoints.put("profile", "GET /api/users/me - 获取当前用户信息");
        userEndpoints.put("updateProfile", "PUT /api/users/{userId}/profile - 更新用户资料");
        userEndpoints.put("changePassword", "PUT /api/users/{userId}/password - 修改密码");
        userEndpoints.put("match", "POST /api/users/{userId}/match - 通过邀请码匹配情侣");
        userEndpoints.put("couple", "GET /api/users/me/couple - 获取当前用户的情侣信息");
        userEndpoints.put("breakUp", "DELETE /api/users/{userId}/couple - 解除情侣关系");
        userEndpoints.put("invitationCode", "GET /api/users/me/invitation-code - 获取邀请码");
        userEndpoints.put("stats", "GET /api/users/me/stats - 获取用户统计信息");
        userEndpoints.put("search", "GET /api/users/search - 搜索用户");
        userEndpoints.put("checkUsername", "GET /api/users/check-username - 检查用户名可用性");
        userEndpoints.put("checkEmail", "GET /api/users/check-email - 检查邮箱可用性");
        endpoints.put("users", userEndpoints);
        
        // 头像相关接口
        Map<String, Object> avatarEndpoints = new HashMap<>();
        avatarEndpoints.put("upload", "POST /api/users/{userId}/avatar/upload - 上传头像");
        avatarEndpoints.put("get", "GET /api/users/{userId}/avatar - 获取头像URL");
        avatarEndpoints.put("delete", "DELETE /api/users/{userId}/avatar - 删除头像");
        endpoints.put("avatars", avatarEndpoints);
        
        // 情侣相关接口
        Map<String, Object> coupleEndpoints = new HashMap<>();
        coupleEndpoints.put("details", "GET /api/couples/me - 获取情侣详细信息");
        coupleEndpoints.put("status", "GET /api/couples/{coupleId}/status - 获取情侣状态");
        coupleEndpoints.put("updateStatus", "PUT /api/couples/{coupleId}/status - 更新情侣状态");
        coupleEndpoints.put("stats", "GET /api/couples/me/stats - 获取情侣统计信息");
        coupleEndpoints.put("members", "GET /api/couples/me/members - 获取情侣成员信息");
        coupleEndpoints.put("duration", "GET /api/couples/me/duration - 获取关系时长");
        coupleEndpoints.put("milestones", "GET /api/couples/me/milestones - 获取情侣里程碑");
        endpoints.put("couples", coupleEndpoints);
        
        // 恋爱信息相关接口
        Map<String, Object> loveInfoEndpoints = new HashMap<>();
        loveInfoEndpoints.put("get", "GET /api/couples/my/love-info - 获取恋爱信息");
        loveInfoEndpoints.put("update", "PUT /api/couples/{coupleId}/love-start-date - 更新恋爱开始日期");
        endpoints.put("loveInfo", loveInfoEndpoints);
        
        // 纪念日相关接口
        Map<String, Object> anniversaryEndpoints = new HashMap<>();
        anniversaryEndpoints.put("list", "GET /api/anniversaries/my - 获取我的纪念日");
        anniversaryEndpoints.put("create", "POST /api/anniversaries - 创建纪念日");
        anniversaryEndpoints.put("update", "PUT /api/anniversaries/{id} - 更新纪念日");
        anniversaryEndpoints.put("delete", "DELETE /api/anniversaries/{id} - 删除纪念日");
        anniversaryEndpoints.put("search", "GET /api/anniversaries/search - 搜索纪念日");
        anniversaryEndpoints.put("upcoming", "GET /api/anniversaries/couple/{coupleId}/upcoming - 获取即将到来的纪念日");
        anniversaryEndpoints.put("overview", "GET /api/anniversaries/couple/{coupleId}/overview - 获取纪念日概览");
        anniversaryEndpoints.put("stats", "GET /api/anniversaries/{id}/stats - 获取纪念日统计");
        anniversaryEndpoints.put("notifications", "GET /api/anniversaries/couple/{coupleId}/notifications - 获取推送通知数据");
        anniversaryEndpoints.put("toggleNotification", "PUT /api/anniversaries/{id}/notification-toggle - 切换推送状态");
        anniversaryEndpoints.put("batchNotification", "PUT /api/anniversaries/batch-notification - 批量设置推送");
        endpoints.put("anniversaries", anniversaryEndpoints);
        
        overview.put("endpoints", endpoints);
        
        return overview;
    }
    
    /**
     * 获取用户API详细文档
     * GET /api/docs/users
     */
    @GetMapping("/users")
    public Map<String, Object> getUserApiDocs() {
        Map<String, Object> docs = new HashMap<>();
        docs.put("title", "用户管理API");
        docs.put("description", "用户注册、登录、资料管理等接口");
        
        Map<String, Object> apis = new HashMap<>();
        
        // 注册接口
        Map<String, Object> register = new HashMap<>();
        register.put("method", "POST");
        register.put("url", "/api/users/register");
        register.put("description", "用户注册");
        register.put("requestBody", Map.of(
            "username", "用户名（必填）",
            "password", "密码（必填）",
            "nickname", "昵称（必填）",
            "email", "邮箱（可选）",
            "gender", "性别：MALE/FEMALE/OTHER（可选）"
        ));
        register.put("response", Map.of(
            "success", "boolean - 是否成功",
            "message", "string - 响应消息",
            "user", "object - 用户信息（成功时）"
        ));
        apis.put("register", register);
        
        // 登录接口
        Map<String, Object> login = new HashMap<>();
        login.put("method", "POST");
        login.put("url", "/api/users/login");
        login.put("description", "用户登录");
        login.put("requestBody", Map.of(
            "identifier", "用户名或邮箱（必填）",
            "password", "密码（必填）"
        ));
        login.put("response", Map.of(
            "success", "boolean - 是否成功",
            "message", "string - 响应消息",
            "token", "string - JWT令牌（成功时）",
            "user", "object - 用户信息（成功时）"
        ));
        apis.put("login", login);
        
        // 获取当前用户信息
        Map<String, Object> getMe = new HashMap<>();
        getMe.put("method", "GET");
        getMe.put("url", "/api/users/me");
        getMe.put("description", "获取当前用户信息");
        getMe.put("headers", Map.of("Authorization", "Bearer {token}"));
        getMe.put("response", Map.of(
            "success", "boolean - 是否成功",
            "user", "object - 用户信息"
        ));
        apis.put("getMe", getMe);
        
        docs.put("apis", apis);
        
        return docs;
    }
    
    /**
     * 获取纪念日API详细文档
     * GET /api/docs/anniversaries
     */
    @GetMapping("/anniversaries")
    public Map<String, Object> getAnniversaryApiDocs() {
        Map<String, Object> docs = new HashMap<>();
        docs.put("title", "纪念日管理API");
        docs.put("description", "纪念日的增删改查、推送设置等接口");
        
        Map<String, Object> apis = new HashMap<>();
        
        // 获取我的纪念日
        Map<String, Object> getMy = new HashMap<>();
        getMy.put("method", "GET");
        getMy.put("url", "/api/anniversaries/my");
        getMy.put("description", "获取当前用户的纪念日列表");
        getMy.put("headers", Map.of("Authorization", "Bearer {token}"));
        getMy.put("response", "List<Anniversary> - 纪念日列表");
        apis.put("getMy", getMy);
        
        // 创建纪念日
        Map<String, Object> create = new HashMap<>();
        create.put("method", "POST");
        create.put("url", "/api/anniversaries");
        create.put("description", "创建新纪念日");
        create.put("headers", Map.of("Authorization", "Bearer {token}"));
        create.put("requestBody", Map.of(
            "name", "纪念日名称（必填）",
            "date", "纪念日日期 YYYY-MM-DD（必填）",
            "description", "描述（可选）",
            "isImportant", "boolean - 是否重要（可选）",
            "enableNotification", "boolean - 是否启用推送（可选）"
        ));
        create.put("response", "Anniversary - 创建的纪念日");
        apis.put("create", create);
        
        // 获取即将到来的纪念日
        Map<String, Object> upcoming = new HashMap<>();
        upcoming.put("method", "GET");
        upcoming.put("url", "/api/anniversaries/couple/{coupleId}/upcoming/{days}");
        upcoming.put("description", "获取指定天数内即将到来的纪念日");
        upcoming.put("parameters", Map.of(
            "coupleId", "情侣ID",
            "days", "天数（0表示今天）"
        ));
        upcoming.put("response", "List<Anniversary> - 纪念日列表");
        apis.put("upcoming", upcoming);
        
        docs.put("apis", apis);
        
        return docs;
    }
    
    /**
     * 获取认证说明
     * GET /api/docs/auth
     */
    @GetMapping("/auth")
    public Map<String, Object> getAuthDocs() {
        Map<String, Object> docs = new HashMap<>();
        docs.put("title", "认证说明");
        docs.put("description", "JWT Token认证机制说明");
        
        docs.put("authentication", Map.of(
            "type", "JWT Bearer Token",
            "header", "Authorization: Bearer {token}",
            "tokenFormat", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
            "expiration", "24小时"
        ));
        
        docs.put("publicEndpoints", new String[]{
            "POST /api/users/register",
            "POST /api/users/login",
            "GET /api/users/check-username",
            "GET /api/users/check-email"
        });
        
        docs.put("protectedEndpoints", "除公开接口外的所有接口都需要JWT认证");
        
        return docs;
    }
    
    /**
     * 获取错误码说明
     * GET /api/docs/errors
     */
    @GetMapping("/errors")
    public Map<String, Object> getErrorDocs() {
        Map<String, Object> docs = new HashMap<>();
        docs.put("title", "错误码说明");
        docs.put("description", "API错误响应格式和常见错误码");
        
        Map<String, Object> errorFormat = new HashMap<>();
        errorFormat.put("success", "false");
        errorFormat.put("message", "错误描述信息");
        errorFormat.put("error", "错误类型");
        errorFormat.put("timestamp", "错误发生时间戳");
        docs.put("errorFormat", errorFormat);
        
        Map<String, Object> commonErrors = new HashMap<>();
        commonErrors.put("400", "请求参数错误");
        commonErrors.put("401", "未授权，需要登录");
        commonErrors.put("403", "禁止访问，权限不足");
        commonErrors.put("404", "资源不存在");
        commonErrors.put("413", "文件大小超限");
        commonErrors.put("500", "服务器内部错误");
        docs.put("commonErrors", commonErrors);
        
        return docs;
    }
} 