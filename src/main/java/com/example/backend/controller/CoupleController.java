package com.example.backend.controller;

import com.example.backend.service.CoupleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 情侣管理控制器
 * 提供情侣信息管理、状态查询等API接口
 */
@RestController
@RequestMapping("/api/couples")
public class CoupleController {
    
    @Autowired
    private CoupleService coupleService;
    
    /**
     * 获取情侣详细信息
     * GET /api/couples/{coupleId}
     */
    @GetMapping("/{coupleId}")
    public Map<String, Object> getCoupleDetails(@PathVariable Long coupleId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return coupleService.getCoupleDetails(coupleId, userId);
    }
    
    /**
     * 获取当前用户的情侣详细信息
     * GET /api/couples/me
     */
    @GetMapping("/me")
    public Map<String, Object> getMyCoupleDetails(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return coupleService.getCoupleDetailsByUserId(userId);
    }
    
    /**
     * 获取情侣状态
     * GET /api/couples/{coupleId}/status
     */
    @GetMapping("/{coupleId}/status")
    public Map<String, Object> getCoupleStatus(@PathVariable Long coupleId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return coupleService.getCoupleStatus(coupleId, userId);
    }
    
    /**
     * 更新情侣状态
     * PUT /api/couples/{coupleId}/status
     * {
     *   "status": "ACTIVE"  // 或 "INACTIVE"
     * }
     */
    @PutMapping("/{coupleId}/status")
    public Map<String, Object> updateCoupleStatus(
            @PathVariable Long coupleId,
            @RequestBody Map<String, String> statusData,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String status = statusData.get("status");
        return coupleService.updateCoupleStatus(coupleId, userId, status);
    }
    
    /**
     * 获取情侣统计信息
     * GET /api/couples/{coupleId}/stats
     */
    @GetMapping("/{coupleId}/stats")
    public Map<String, Object> getCoupleStats(@PathVariable Long coupleId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return coupleService.getCoupleStats(coupleId, userId);
    }
    
    /**
     * 获取当前用户的情侣统计信息
     * GET /api/couples/me/stats
     */
    @GetMapping("/me/stats")
    public Map<String, Object> getMyCoupleStats(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return coupleService.getCoupleStatsByUserId(userId);
    }
    
    /**
     * 获取情侣成员信息
     * GET /api/couples/{coupleId}/members
     */
    @GetMapping("/{coupleId}/members")
    public Map<String, Object> getCoupleMembers(@PathVariable Long coupleId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return coupleService.getCoupleMembers(coupleId, userId);
    }
    
    /**
     * 获取当前用户的情侣成员信息
     * GET /api/couples/me/members
     */
    @GetMapping("/me/members")
    public Map<String, Object> getMyCoupleMembers(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return coupleService.getCoupleMembersByUserId(userId);
    }
    
    /**
     * 获取情侣关系时长
     * GET /api/couples/{coupleId}/duration
     */
    @GetMapping("/{coupleId}/duration")
    public Map<String, Object> getCoupleDuration(@PathVariable Long coupleId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return coupleService.getCoupleDuration(coupleId, userId);
    }
    
    /**
     * 获取当前用户的情侣关系时长
     * GET /api/couples/me/duration
     */
    @GetMapping("/me/duration")
    public Map<String, Object> getMyCoupleDuration(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return coupleService.getCoupleDurationByUserId(userId);
    }
    
    /**
     * 获取情侣里程碑
     * GET /api/couples/{coupleId}/milestones
     */
    @GetMapping("/{coupleId}/milestones")
    public Map<String, Object> getCoupleMilestones(@PathVariable Long coupleId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return coupleService.getCoupleMilestones(coupleId, userId);
    }
    
    /**
     * 获取当前用户的情侣里程碑
     * GET /api/couples/me/milestones
     */
    @GetMapping("/me/milestones")
    public Map<String, Object> getMyCoupleMilestones(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return coupleService.getCoupleMilestonesByUserId(userId);
    }
} 