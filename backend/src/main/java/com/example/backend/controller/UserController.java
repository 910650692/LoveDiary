package com.example.backend.controller;

import com.example.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 用户控制器
 * 提供用户注册、登录、匹配等API接口
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    /**
     * 用户注册
     * POST /api/users/register
     * {
     *   "username": "testuser",
     *   "password": "123456",
     *   "nickname": "测试用户",
     *   "email": "test@example.com"
     * }
     */
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> registerData) {
        return userService.register(registerData);
    }
    
    /**
     * 用户登录
     * POST /api/users/login
     * {
     *   "identifier": "testuser",  // 用户名或邮箱
     *   "password": "123456"
     * }
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> loginData) {
        String identifier = loginData.get("identifier");
        String password = loginData.get("password");
        return userService.login(identifier, password);
    }
    
    /**
     * 通过邀请码匹配情侣
     * POST /api/users/{userId}/match
     * {
     *   "invitationCode": "ABC123"
     * }
     */
    @PostMapping("/{userId}/match")
    public Map<String, Object> matchWithInvitationCode(
            @PathVariable Long userId, 
            @RequestBody Map<String, String> matchData) {
        String invitationCode = matchData.get("invitationCode");
        return userService.matchWithInvitationCode(userId, invitationCode);
    }
    
    /**
     * 获取用户信息
     * GET /api/users/{userId}
     */
    @GetMapping("/{userId}")
    public Map<String, Object> getUserInfo(@PathVariable Long userId) {
        Map<String, Object> userInfo = userService.getUserInfo(userId);
        if (userInfo != null) {
            return Map.of("success", true, "user", userInfo);
        } else {
            return Map.of("success", false, "message", "用户不存在");
        }
    }
    
    /**
     * 获取当前用户信息
     * GET /api/users/me
     */
    @GetMapping("/me")
    public Map<String, Object> getCurrentUserInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Map<String, Object> userInfo = userService.getUserInfo(userId);
        if (userInfo != null) {
            return Map.of("success", true, "user", userInfo);
        } else {
            return Map.of("success", false, "message", "用户不存在");
        }
    }
    
    /**
     * 更新用户资料
     * PUT /api/users/{userId}/profile
     * {
     *   "nickname": "新昵称",
     *   "email": "newemail@example.com",
     *   "phone": "13800138000",
     *   "gender": "MALE",
     *   "birthDate": "1990-01-01"
     * }
     */
    @PutMapping("/{userId}/profile")
    public Map<String, Object> updateProfile(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> profileData,
            HttpServletRequest request) {
        
        // 验证权限：只能修改自己的资料
        Long currentUserId = (Long) request.getAttribute("userId");
        if (!currentUserId.equals(userId)) {
            return Map.of(
                "success", false,
                "message", "只能修改自己的资料"
            );
        }
        
        return userService.updateProfile(userId, profileData);
    }
    
    /**
     * 修改密码
     * PUT /api/users/{userId}/password
     * {
     *   "oldPassword": "123456",
     *   "newPassword": "newpassword123"
     * }
     */
    @PutMapping("/{userId}/password")
    public Map<String, Object> changePassword(
            @PathVariable Long userId,
            @RequestBody Map<String, String> passwordData,
            HttpServletRequest request) {
        
        // 验证权限：只能修改自己的密码
        Long currentUserId = (Long) request.getAttribute("userId");
        if (!currentUserId.equals(userId)) {
            return Map.of(
                "success", false,
                "message", "只能修改自己的密码"
            );
        }
        
        String oldPassword = passwordData.get("oldPassword");
        String newPassword = passwordData.get("newPassword");
        return userService.changePassword(userId, oldPassword, newPassword);
    }
    
    /**
     * 获取用户的情侣信息
     * GET /api/users/{userId}/couple
     */
    @GetMapping("/{userId}/couple")
    public Map<String, Object> getCoupleInfo(@PathVariable Long userId) {
        Map<String, Object> coupleInfo = userService.getCoupleInfo(userId);
        if (coupleInfo != null) {
            return Map.of("success", true, "couple", coupleInfo);
        } else {
            return Map.of("success", false, "message", "没有找到情侣信息");
        }
    }
    
    /**
     * 获取当前用户的情侣信息
     * GET /api/users/me/couple
     */
    @GetMapping("/me/couple")
    public Map<String, Object> getMyCoupleInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Map<String, Object> coupleInfo = userService.getCoupleInfo(userId);
        if (coupleInfo != null) {
            return Map.of("success", true, "couple", coupleInfo);
        } else {
            return Map.of("success", false, "message", "没有找到情侣信息");
        }
    }
    
    /**
     * 解除情侣关系
     * DELETE /api/users/{userId}/couple
     */
    @DeleteMapping("/{userId}/couple")
    public Map<String, Object> breakUp(@PathVariable Long userId) {
        return userService.breakUp(userId);
    }
    
    /**
     * 获取用户的邀请码
     * GET /api/users/{userId}/invitation-code
     */
    @GetMapping("/{userId}/invitation-code")
    public Map<String, Object> getInvitationCode(@PathVariable Long userId) {
        Map<String, Object> userInfo = userService.getUserInfo(userId);
        if (userInfo != null) {
            return Map.of(
                "success", true, 
                "invitationCode", userInfo.get("invitationCode"),
                "message", "您的邀请码是：" + userInfo.get("invitationCode")
            );
        } else {
            return Map.of("success", false, "message", "用户不存在");
        }
    }
    
    /**
     * 获取当前用户的邀请码
     * GET /api/users/me/invitation-code
     */
    @GetMapping("/me/invitation-code")
    public Map<String, Object> getMyInvitationCode(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Map<String, Object> userInfo = userService.getUserInfo(userId);
        if (userInfo != null) {
            return Map.of(
                "success", true, 
                "invitationCode", userInfo.get("invitationCode"),
                "message", "您的邀请码是：" + userInfo.get("invitationCode")
            );
        } else {
            return Map.of("success", false, "message", "用户不存在");
        }
    }
    
    /**
     * 获取用户统计信息
     * GET /api/users/{userId}/stats
     */
    @GetMapping("/{userId}/stats")
    public Map<String, Object> getUserStats(@PathVariable Long userId) {
        return userService.getUserStats(userId);
    }
    
    /**
     * 获取当前用户统计信息
     * GET /api/users/me/stats
     */
    @GetMapping("/me/stats")
    public Map<String, Object> getMyStats(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return userService.getUserStats(userId);
    }
    
    /**
     * 搜索用户（通过用户名或昵称）
     * GET /api/users/search?keyword=关键词
     */
    @GetMapping("/search")
    public Map<String, Object> searchUsers(@RequestParam String keyword) {
        return userService.searchUsers(keyword);
    }
    
    /**
     * 检查用户名是否可用
     * GET /api/users/check-username?username=testuser
     */
    @GetMapping("/check-username")
    public Map<String, Object> checkUsernameAvailability(@RequestParam String username) {
        boolean available = userService.isUsernameAvailable(username);
        return Map.of(
            "success", true,
            "available", available,
            "message", available ? "用户名可用" : "用户名已被使用"
        );
    }
    
    /**
     * 检查邮箱是否可用
     * GET /api/users/check-email?email=test@example.com
     */
    @GetMapping("/check-email")
    public Map<String, Object> checkEmailAvailability(@RequestParam String email) {
        boolean available = userService.isEmailAvailable(email);
        return Map.of(
            "success", true,
            "available", available,
            "message", available ? "邮箱可用" : "邮箱已被使用"
        );
    }
} 