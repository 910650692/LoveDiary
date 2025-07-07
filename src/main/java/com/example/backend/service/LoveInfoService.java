package com.example.backend.service;

import com.example.backend.dto.LoveInfoDTO;
import com.example.backend.model.Couple;
import com.example.backend.respository.CoupleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 恋爱信息服务类
 * 处理恋爱开始日期的获取、更新和计算
 */
@Service
public class LoveInfoService {
    
    @Autowired
    private CoupleRepository coupleRepository;
    
    /**
     * 获取情侣的恋爱信息
     */
    public Map<String, Object> getLoveInfo(Long coupleId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<Couple> coupleOpt = coupleRepository.findById(coupleId);
            if (!coupleOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "情侣关系不存在");
                return result;
            }
            
            Couple couple = coupleOpt.get();
            if (!couple.isActive()) {
                result.put("success", false);
                result.put("message", "情侣关系已结束");
                return result;
            }
            
            LoveInfoDTO loveInfo = calculateLoveInfo(couple);
            
            result.put("success", true);
            result.put("data", loveInfo);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取恋爱信息失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 更新恋爱开始日期
     */
    public Map<String, Object> updateLoveStartDate(Long coupleId, LocalDate loveStartDate, Long currentUserId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 验证情侣关系
            Optional<Couple> coupleOpt = coupleRepository.findById(coupleId);
            if (!coupleOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "情侣关系不存在");
                return result;
            }
            
            Couple couple = coupleOpt.get();
            if (!couple.isActive()) {
                result.put("success", false);
                result.put("message", "情侣关系已结束");
                return result;
            }
            
            // 验证权限：只有情侣双方可以修改
            if (!couple.containsUser(currentUserId)) {
                result.put("success", false);
                result.put("message", "只能修改自己的恋爱信息");
                return result;
            }
            
            // 验证日期
            if (loveStartDate == null) {
                result.put("success", false);
                result.put("message", "恋爱开始日期不能为空");
                return result;
            }
            
            if (loveStartDate.isAfter(LocalDate.now())) {
                result.put("success", false);
                result.put("message", "恋爱开始日期不能是未来日期");
                return result;
            }
            
            
            // 更新恋爱开始日期
            couple.setLoveStartDate(loveStartDate);
            coupleRepository.save(couple);
            
            // 返回更新后的恋爱信息
            LoveInfoDTO loveInfo = calculateLoveInfo(couple);
            
            result.put("success", true);
            result.put("message", "恋爱开始日期更新成功");
            result.put("data", loveInfo);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "更新恋爱开始日期失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 计算恋爱信息
     */
    private LoveInfoDTO calculateLoveInfo(Couple couple) {
        LoveInfoDTO loveInfo = new LoveInfoDTO();
        loveInfo.setLoveStartDate(couple.getLoveStartDate());
        
        if (couple.getLoveStartDate() != null) {
            LocalDate today = LocalDate.now();
            LocalDate startDate = couple.getLoveStartDate();
            
            // 计算恋爱天数
            long days = ChronoUnit.DAYS.between(startDate, today);
            loveInfo.setLoveDays(days);
            
            // 计算恋爱月数
            long months = ChronoUnit.MONTHS.between(startDate, today);
            loveInfo.setLoveMonths(months);
            
            // 计算恋爱年数
            long years = ChronoUnit.YEARS.between(startDate, today);
            loveInfo.setLoveYears(years);
        }
        
        return loveInfo;
    }
    
    /**
     * 根据用户ID获取恋爱信息
     */
    public Map<String, Object> getLoveInfoByUserId(Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<Couple> coupleOpt = coupleRepository.findByUserId(userId);
            if (!coupleOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "用户未处于情侣关系中");
                return result;
            }
            
            Couple couple = coupleOpt.get();
            LoveInfoDTO loveInfo = calculateLoveInfo(couple);
            
            result.put("success", true);
            result.put("data", loveInfo);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取恋爱信息失败：" + e.getMessage());
        }
        
        return result;
    }
} 