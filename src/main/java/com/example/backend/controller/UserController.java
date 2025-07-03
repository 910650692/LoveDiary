package com.example.backend.controller;

import com.example.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
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
} 