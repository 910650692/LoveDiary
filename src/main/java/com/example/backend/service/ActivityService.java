package com.example.backend.service;

import com.example.backend.model.Activity;
import com.example.backend.model.Couple;
import com.example.backend.respository.ActivityRepository;
import com.example.backend.respository.CoupleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * 用户动态服务类
 * 处理用户动态的业务逻辑，包括创建、查询、统计等功能
 */
@Service
@Transactional
public class ActivityService {
    
    @Autowired
    private ActivityRepository activityRepository;
    
    @Autowired
    private CoupleRepository coupleRepository;
    
    /**
     * 创建动态记录
     */
    public Activity createActivity(Long coupleId, Long userId, Activity.ActivityType activityType, String title) {
        return createActivity(coupleId, userId, activityType, title, null, null, null);
    }
    
    /**
     * 创建带描述的动态记录
     */
    public Activity createActivity(Long coupleId, Long userId, Activity.ActivityType activityType, 
                                 String title, String description) {
        return createActivity(coupleId, userId, activityType, title, description, null, null);
    }
    
    /**
     * 创建完整的动态记录
     */
    public Activity createActivity(Long coupleId, Long userId, Activity.ActivityType activityType, 
                                 String title, String description, Long referenceId, String referenceType) {
        // 验证情侣关系
        Optional<Couple> couple = coupleRepository.findById(coupleId);
        if (couple.isEmpty() || couple.get().getStatus() != Couple.CoupleStatus.ACTIVE) {
            throw new RuntimeException("Invalid couple relationship");
        }
        
        // 验证用户是否属于该情侣
        Couple coupleEntity = couple.get();
        if (!Objects.equals(coupleEntity.getUser1Id(), userId) && !Objects.equals(coupleEntity.getUser2Id(), userId)) {
            throw new RuntimeException("User does not belong to this couple");
        }
        
        Activity activity = new Activity(coupleId, userId, activityType, title, description);
        
        // 设置引用信息
        if (referenceId != null && referenceType != null) {
            activity.setReference(referenceId, referenceType);
        }
        
        // 设置图标
        activity.setIconFromType();
        
        return activityRepository.save(activity);
    }
    
    /**
     * 获取情侣的最近动态（分页）
     */
    @Transactional(readOnly = true)
    public Page<Activity> getRecentActivities(Long coupleId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return activityRepository.findByCoupleIdAndIsDeletedFalseOrderByCreatedAtDesc(coupleId, pageable);
    }
    
    /**
     * 获取情侣的最近动态（限制数量）
     */
    @Transactional(readOnly = true)
    public List<Activity> getRecentActivities(Long coupleId, int limit) {
        if (limit <= 20) {
            return activityRepository.findTop20ByCoupleIdAndIsDeletedFalseOrderByCreatedAtDesc(coupleId)
                    .stream().limit(limit).collect(ArrayList::new, (list, item) -> list.add(item), (list1, list2) -> list1.addAll(list2));
        } else {
            Pageable pageable = PageRequest.of(0, limit);
            return activityRepository.findByCoupleIdAndIsDeletedFalseOrderByCreatedAtDesc(coupleId, pageable).getContent();
        }
    }
    
    /**
     * 获取情侣的最近动态（默认20条）
     */
    @Transactional(readOnly = true)
    public List<Activity> getRecentActivities(Long coupleId) {
        return activityRepository.findTop20ByCoupleIdAndIsDeletedFalseOrderByCreatedAtDesc(coupleId);
    }
    
    /**
     * 获取最近7天的动态
     */
    @Transactional(readOnly = true)
    public List<Activity> getWeeklyActivities(Long coupleId) {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        return activityRepository.findRecentActivities(coupleId, sevenDaysAgo);
    }
    
    /**
     * 获取今天的动态
     */
    @Transactional(readOnly = true)
    public List<Activity> getTodayActivities(Long coupleId) {
        return activityRepository.findTodayActivities(coupleId);
    }
    
    /**
     * 根据动态类型获取动态
     */
    @Transactional(readOnly = true)
    public List<Activity> getActivitiesByType(Long coupleId, Activity.ActivityType activityType) {
        return activityRepository.findByCoupleIdAndActivityTypeAndIsDeletedFalseOrderByCreatedAtDesc(coupleId, activityType);
    }
    
    /**
     * 根据用户获取动态
     */
    @Transactional(readOnly = true)
    public List<Activity> getActivitiesByUser(Long coupleId, Long userId) {
        return activityRepository.findByCoupleIdAndUserIdAndIsDeletedFalseOrderByCreatedAtDesc(coupleId, userId);
    }
    
    /**
     * 根据时间范围获取动态
     */
    @Transactional(readOnly = true)
    public List<Activity> getActivitiesByDateRange(Long coupleId, LocalDateTime startDate, LocalDateTime endDate) {
        return activityRepository.findByCoupleIdAndDateRange(coupleId, startDate, endDate);
    }
    
    /**
     * 删除动态（软删除）
     */
    public boolean deleteActivity(Long activityId, Long coupleId) {
        Optional<Activity> activityOpt = activityRepository.findByIdAndIsDeletedFalse(activityId);
        if (activityOpt.isEmpty()) {
            return false;
        }
        
        Activity activity = activityOpt.get();
        if (!Objects.equals(activity.getCoupleId(), coupleId)) {
            throw new RuntimeException("Activity does not belong to this couple");
        }
        
        activity.markAsDeleted();
        activityRepository.save(activity);
        return true;
    }
    
    /**
     * 根据引用删除相关动态
     */
    public void deleteActivitiesByReference(Long referenceId, String referenceType) {
        activityRepository.softDeleteByReference(referenceId, referenceType, LocalDateTime.now());
    }
    
    /**
     * 获取动态统计信息
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getActivityStatistics(Long coupleId) {
        Map<String, Object> statistics = new HashMap<>();
        
        // 总动态数
        long totalCount = activityRepository.countByCoupleIdAndIsDeletedFalse(coupleId);
        statistics.put("totalCount", totalCount);
        
        // 今天动态数
        List<Activity> todayActivities = getTodayActivities(coupleId);
        statistics.put("todayCount", todayActivities.size());
        
        // 本周动态数
        List<Activity> weeklyActivities = getWeeklyActivities(coupleId);
        statistics.put("weeklyCount", weeklyActivities.size());
        
        // 活跃天数
        long activeDays = activityRepository.getActiveDaysCount(coupleId);
        statistics.put("activeDays", activeDays);
        
        // 动态类型分布
        List<Object[]> typeStatistics = activityRepository.getActivityTypeStatistics(coupleId);
        Map<String, Long> typeDistribution = new HashMap<>();
        for (Object[] row : typeStatistics) {
            Activity.ActivityType type = (Activity.ActivityType) row[0];
            Long count = (Long) row[1];
            typeDistribution.put(type.name(), count);
        }
        statistics.put("typeDistribution", typeDistribution);
        
        return statistics;
    }
    
    /**
     * 清理旧动态（超过指定天数的动态）
     */
    public void cleanOldActivities(Long coupleId, int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        List<Activity> oldActivities = activityRepository.findByCoupleIdAndCreatedAtBeforeAndIsDeletedFalse(coupleId, cutoffDate);
        
        for (Activity activity : oldActivities) {
            activity.markAsDeleted();
        }
        
        activityRepository.saveAll(oldActivities);
    }
    
    /**
     * 构建动态摘要（用于首页展示）
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getActivitySummary(Long coupleId, int limit) {
        List<Activity> activities = getRecentActivities(coupleId, limit);
        List<Map<String, Object>> summary = new ArrayList<>();
        
        for (Activity activity : activities) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", activity.getId());
            item.put("icon", activity.getIcon());
            item.put("title", activity.getTitle());
            item.put("description", activity.getDescription());
            item.put("time", formatRelativeTime(activity.getCreatedAt()));
            item.put("activityType", activity.getActivityType().getDisplayName());
            summary.add(item);
        }
        
        return summary;
    }
    
    /**
     * 格式化相对时间
     */
    private String formatRelativeTime(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now();
        long minutesDiff = java.time.Duration.between(dateTime, now).toMinutes();
        
        if (minutesDiff < 1) {
            return "刚刚";
        } else if (minutesDiff < 60) {
            return minutesDiff + "分钟前";
        } else if (minutesDiff < 24 * 60) {
            return (minutesDiff / 60) + "小时前";
        } else if (minutesDiff < 30 * 24 * 60) {
            return (minutesDiff / (24 * 60)) + "天前";
        } else {
            return dateTime.toLocalDate().toString();
        }
    }
    
    /**
     * 验证动态访问权限
     */
    @Transactional(readOnly = true)
    public boolean hasAccessToActivity(Long activityId, Long coupleId) {
        return activityRepository.existsByIdAndCoupleIdAndIsDeletedFalse(activityId, coupleId);
    }
}