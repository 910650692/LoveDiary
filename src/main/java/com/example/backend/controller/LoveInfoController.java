package com.example.backend.controller;

import com.example.backend.dto.LoveInfoDTO;
import com.example.backend.service.LoveInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDate;
import java.util.Map;

/**
 * 恋爱信息控制器
 * 提供恋爱开始日期的获取和更新API
 */
@RestController
@RequestMapping("/api/couples")
public class LoveInfoController {
    
    @Autowired
    private LoveInfoService loveInfoService;
    
    /**
     * 获取情侣的恋爱信息
     * GET /api/couples/{coupleId}/love-info
     */
    @GetMapping("/{coupleId}/love-info")
    public Map<String, Object> getLoveInfo(@PathVariable Long coupleId) {
        return loveInfoService.getLoveInfo(coupleId);
    }
    
    /**
     * 更新恋爱开始日期
     * PUT /api/couples/{coupleId}/love-start-date
     */
    @PutMapping("/{coupleId}/love-start-date")
    public Map<String, Object> updateLoveStartDate(
            @PathVariable Long coupleId,
            @RequestBody LoveInfoDTO loveInfoDTO,
            HttpServletRequest request) {
        
        Long currentUserId = (Long) request.getAttribute("userId");
        return loveInfoService.updateLoveStartDate(coupleId, loveInfoDTO.getLoveStartDate(), currentUserId);
    }
    
    /**
     * 根据当前用户获取恋爱信息
     * GET /api/couples/my/love-info
     */
    @GetMapping("/my/love-info")
    public Map<String, Object> getMyLoveInfo(HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("userId");
        return loveInfoService.getLoveInfoByUserId(currentUserId);
    }
} 