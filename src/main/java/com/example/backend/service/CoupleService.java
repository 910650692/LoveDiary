package com.example.backend.service;

import com.example.backend.model.Couple;
import com.example.backend.model.User;
import com.example.backend.respository.CoupleRepository;
import com.example.backend.respository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 情侣服务类
 * 处理情侣信息管理、状态查询等业务逻辑
 */
@Service
@Transactional
public class CoupleService {
    
    @Autowired
    private CoupleRepository coupleRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * 获取情侣详细信息
     */
    public Map<String, Object> getCoupleDetails(Long coupleId, Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<Couple> coupleOpt = coupleRepository.findById(coupleId);
            if (!coupleOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "情侣关系不存在");
                return result;
            }
            
            Couple couple = coupleOpt.get();
            
            // 验证用户是否属于这个情侣
            if (!couple.containsUser(userId)) {
                result.put("success", false);
                result.put("message", "无权访问此情侣信息");
                return result;
            }
            
            // 获取情侣成员信息
            Optional<User> user1Opt = userRepository.findById(couple.getUser1Id());
            Optional<User> user2Opt = userRepository.findById(couple.getUser2Id());
            
            Map<String, Object> coupleInfo = new HashMap<>();
            coupleInfo.put("id", couple.getId());
            coupleInfo.put("status", couple.getStatus().name());
            coupleInfo.put("loveStartDate", couple.getLoveStartDate());
            coupleInfo.put("matchDate", couple.getMatchDate());
            coupleInfo.put("createdAt", couple.getCreatedAt());
            
            if (user1Opt.isPresent()) {
                coupleInfo.put("user1", createUserInfo(user1Opt.get()));
            }
            if (user2Opt.isPresent()) {
                coupleInfo.put("user2", createUserInfo(user2Opt.get()));
            }
            
            result.put("success", true);
            result.put("couple", coupleInfo);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取情侣信息失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 根据用户ID获取情侣详细信息
     */
    public Map<String, Object> getCoupleDetailsByUserId(Long userId) {
        Optional<Couple> coupleOpt = coupleRepository.findByUserId(userId);
        if (coupleOpt.isPresent()) {
            return getCoupleDetails(coupleOpt.get().getId(), userId);
        } else {
            return Map.of("success", false, "message", "没有找到情侣信息");
        }
    }
    
    /**
     * 获取情侣状态
     */
    public Map<String, Object> getCoupleStatus(Long coupleId, Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<Couple> coupleOpt = coupleRepository.findById(coupleId);
            if (!coupleOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "情侣关系不存在");
                return result;
            }
            
            Couple couple = coupleOpt.get();
            
            // 验证用户是否属于这个情侣
            if (!couple.containsUser(userId)) {
                result.put("success", false);
                result.put("message", "无权访问此情侣信息");
                return result;
            }
            
            result.put("success", true);
            result.put("status", couple.getStatus().name());
            result.put("isActive", couple.isActive());
            result.put("message", couple.isActive() ? "关系正常" : "关系已结束");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取状态失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 更新情侣状态
     */
    public Map<String, Object> updateCoupleStatus(Long coupleId, Long userId, String status) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<Couple> coupleOpt = coupleRepository.findById(coupleId);
            if (!coupleOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "情侣关系不存在");
                return result;
            }
            
            Couple couple = coupleOpt.get();
            
            // 验证用户是否属于这个情侣
            if (!couple.containsUser(userId)) {
                result.put("success", false);
                result.put("message", "无权修改此情侣信息");
                return result;
            }
            
            // 更新状态
            try {
                Couple.CoupleStatus newStatus = Couple.CoupleStatus.valueOf(status.toUpperCase());
                couple.setStatus(newStatus);
                coupleRepository.save(couple);
                
                result.put("success", true);
                result.put("status", newStatus.name());
                result.put("message", "状态更新成功");
                
            } catch (IllegalArgumentException e) {
                result.put("success", false);
                result.put("message", "状态值无效");
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "更新状态失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取情侣统计信息
     */
    public Map<String, Object> getCoupleStats(Long coupleId, Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<Couple> coupleOpt = coupleRepository.findById(coupleId);
            if (!coupleOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "情侣关系不存在");
                return result;
            }
            
            Couple couple = coupleOpt.get();
            
            // 验证用户是否属于这个情侣
            if (!couple.containsUser(userId)) {
                result.put("success", false);
                result.put("message", "无权访问此情侣信息");
                return result;
            }
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("coupleId", coupleId);
            stats.put("status", couple.getStatus().name());
            stats.put("isActive", couple.isActive());
            stats.put("matchDate", couple.getMatchDate());
            stats.put("loveStartDate", couple.getLoveStartDate());
            
            // 计算关系时长
            if (couple.getMatchDate() != null) {
                long matchDays = ChronoUnit.DAYS.between(couple.getMatchDate().toLocalDate(), LocalDate.now());
                stats.put("matchDays", matchDays);
            }
            
            if (couple.getLoveStartDate() != null) {
                long loveDays = ChronoUnit.DAYS.between(couple.getLoveStartDate(), LocalDate.now());
                stats.put("loveDays", loveDays);
            }
            
            result.put("success", true);
            result.put("stats", stats);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取统计信息失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 根据用户ID获取情侣统计信息
     */
    public Map<String, Object> getCoupleStatsByUserId(Long userId) {
        Optional<Couple> coupleOpt = coupleRepository.findByUserId(userId);
        if (coupleOpt.isPresent()) {
            return getCoupleStats(coupleOpt.get().getId(), userId);
        } else {
            return Map.of("success", false, "message", "没有找到情侣信息");
        }
    }
    
    /**
     * 获取情侣成员信息
     */
    public Map<String, Object> getCoupleMembers(Long coupleId, Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<Couple> coupleOpt = coupleRepository.findById(coupleId);
            if (!coupleOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "情侣关系不存在");
                return result;
            }
            
            Couple couple = coupleOpt.get();
            
            // 验证用户是否属于这个情侣
            if (!couple.containsUser(userId)) {
                result.put("success", false);
                result.put("message", "无权访问此情侣信息");
                return result;
            }
            
            // 获取成员信息
            Optional<User> user1Opt = userRepository.findById(couple.getUser1Id());
            Optional<User> user2Opt = userRepository.findById(couple.getUser2Id());
            
            List<Map<String, Object>> members = new ArrayList<>();
            if (user1Opt.isPresent()) {
                members.add(createUserInfo(user1Opt.get()));
            }
            if (user2Opt.isPresent()) {
                members.add(createUserInfo(user2Opt.get()));
            }
            
            result.put("success", true);
            result.put("members", members);
            result.put("count", members.size());
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取成员信息失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 根据用户ID获取情侣成员信息
     */
    public Map<String, Object> getCoupleMembersByUserId(Long userId) {
        Optional<Couple> coupleOpt = coupleRepository.findByUserId(userId);
        if (coupleOpt.isPresent()) {
            return getCoupleMembers(coupleOpt.get().getId(), userId);
        } else {
            return Map.of("success", false, "message", "没有找到情侣信息");
        }
    }
    
    /**
     * 获取情侣关系时长
     */
    public Map<String, Object> getCoupleDuration(Long coupleId, Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<Couple> coupleOpt = coupleRepository.findById(coupleId);
            if (!coupleOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "情侣关系不存在");
                return result;
            }
            
            Couple couple = coupleOpt.get();
            
            // 验证用户是否属于这个情侣
            if (!couple.containsUser(userId)) {
                result.put("success", false);
                result.put("message", "无权访问此情侣信息");
                return result;
            }
            
            Map<String, Object> duration = new HashMap<>();
            
            // 计算匹配时长
            if (couple.getMatchDate() != null) {
                long matchDays = ChronoUnit.DAYS.between(couple.getMatchDate().toLocalDate(), LocalDate.now());
                long matchWeeks = ChronoUnit.WEEKS.between(couple.getMatchDate().toLocalDate(), LocalDate.now());
                long matchMonths = ChronoUnit.MONTHS.between(couple.getMatchDate().toLocalDate(), LocalDate.now());
                long matchYears = ChronoUnit.YEARS.between(couple.getMatchDate().toLocalDate(), LocalDate.now());
                
                duration.put("matchDays", matchDays);
                duration.put("matchWeeks", matchWeeks);
                duration.put("matchMonths", matchMonths);
                duration.put("matchYears", matchYears);
            }
            
            // 计算恋爱时长
            if (couple.getLoveStartDate() != null) {
                long loveDays = ChronoUnit.DAYS.between(couple.getLoveStartDate(), LocalDate.now());
                long loveWeeks = ChronoUnit.WEEKS.between(couple.getLoveStartDate(), LocalDate.now());
                long loveMonths = ChronoUnit.MONTHS.between(couple.getLoveStartDate(), LocalDate.now());
                long loveYears = ChronoUnit.YEARS.between(couple.getLoveStartDate(), LocalDate.now());
                
                duration.put("loveDays", loveDays);
                duration.put("loveWeeks", loveWeeks);
                duration.put("loveMonths", loveMonths);
                duration.put("loveYears", loveYears);
            }
            
            result.put("success", true);
            result.put("duration", duration);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取关系时长失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 根据用户ID获取情侣关系时长
     */
    public Map<String, Object> getCoupleDurationByUserId(Long userId) {
        Optional<Couple> coupleOpt = coupleRepository.findByUserId(userId);
        if (coupleOpt.isPresent()) {
            return getCoupleDuration(coupleOpt.get().getId(), userId);
        } else {
            return Map.of("success", false, "message", "没有找到情侣信息");
        }
    }
    
    /**
     * 获取情侣里程碑
     */
    public Map<String, Object> getCoupleMilestones(Long coupleId, Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<Couple> coupleOpt = coupleRepository.findById(coupleId);
            if (!coupleOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "情侣关系不存在");
                return result;
            }
            
            Couple couple = coupleOpt.get();
            
            // 验证用户是否属于这个情侣
            if (!couple.containsUser(userId)) {
                result.put("success", false);
                result.put("message", "无权访问此情侣信息");
                return result;
            }
            
            List<Map<String, Object>> milestones = new ArrayList<>();
            
            // 添加匹配里程碑
            if (couple.getMatchDate() != null) {
                Map<String, Object> matchMilestone = new HashMap<>();
                matchMilestone.put("type", "MATCH");
                matchMilestone.put("title", "成为情侣");
                matchMilestone.put("date", couple.getMatchDate());
                matchMilestone.put("description", "通过邀请码成功匹配");
                milestones.add(matchMilestone);
            }
            
            // 添加恋爱开始里程碑
            if (couple.getLoveStartDate() != null) {
                Map<String, Object> loveMilestone = new HashMap<>();
                loveMilestone.put("type", "LOVE_START");
                loveMilestone.put("title", "恋爱开始");
                loveMilestone.put("date", couple.getLoveStartDate());
                loveMilestone.put("description", "正式确定恋爱关系");
                milestones.add(loveMilestone);
            }
            
            // 计算恋爱天数里程碑
            if (couple.getLoveStartDate() != null) {
                long loveDays = ChronoUnit.DAYS.between(couple.getLoveStartDate(), LocalDate.now());
                
                // 添加特殊天数里程碑
                long[] specialDays = {7, 30, 100, 365, 520, 999, 1314};
                for (long day : specialDays) {
                    if (loveDays >= day) {
                        Map<String, Object> milestone = new HashMap<>();
                        milestone.put("type", "SPECIAL_DAY");
                        milestone.put("title", day + "天纪念");
                        milestone.put("days", day);
                        milestone.put("achieved", true);
                        milestone.put("description", "恋爱" + day + "天纪念");
                        milestones.add(milestone);
                    }
                }
            }
            
            result.put("success", true);
            result.put("milestones", milestones);
            result.put("count", milestones.size());
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取里程碑失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 根据用户ID获取情侣里程碑
     */
    public Map<String, Object> getCoupleMilestonesByUserId(Long userId) {
        Optional<Couple> coupleOpt = coupleRepository.findByUserId(userId);
        if (coupleOpt.isPresent()) {
            return getCoupleMilestones(coupleOpt.get().getId(), userId);
        } else {
            return Map.of("success", false, "message", "没有找到情侣信息");
        }
    }
    
    /**
     * 创建用户信息Map（不包含敏感信息）
     */
    private Map<String, Object> createUserInfo(User user) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("email", user.getEmail());
        userInfo.put("gender", user.getGender() != null ? user.getGender().name() : null);
        userInfo.put("birthDate", user.getBirthDate());
        userInfo.put("avatarUrl", user.getAvatarUrl());
        userInfo.put("status", user.getStatus().name());
        userInfo.put("createdAt", user.getCreatedAt());
        return userInfo;
    }
} 