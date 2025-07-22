package com.example.backend.controller;

import com.example.backend.model.Activity;
import com.example.backend.service.ActivityService;
import com.example.backend.config.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户动态控制器
 * 提供动态相关的API接口
 */
@RestController
@RequestMapping("/api/activities")
public class ActivityController {
    
    @Autowired
    private ActivityService activityService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    /**
     * 获取最近动态
     * GET /api/activities/recent?limit=20
     */
    @GetMapping("/recent")
    public ResponseEntity<Map<String, Object>> getRecentActivities(
            @RequestParam(defaultValue = "20") int limit,
            HttpServletRequest request) {
        
        try {
            Long coupleId = jwtUtil.getCoupleIdFromRequest(request);
            if (coupleId == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("未找到情侣关系"));
            }
            
            List<Activity> activities = activityService.getRecentActivities(coupleId, limit);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", activities);
            response.put("total", activities.size());
            response.put("message", "获取最近动态成功");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("获取动态失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取动态摘要（用于首页展示）
     * GET /api/activities/summary?limit=5
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getActivitySummary(
            @RequestParam(defaultValue = "5") int limit,
            HttpServletRequest request) {
        
        try {
            Long coupleId = jwtUtil.getCoupleIdFromRequest(request);
            if (coupleId == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("未找到情侣关系"));
            }
            
            List<Map<String, Object>> summary = activityService.getActivitySummary(coupleId, limit);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", summary);
            response.put("total", summary.size());
            response.put("message", "获取动态摘要成功");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("获取动态摘要失败: " + e.getMessage()));
        }
    }
    
    /**
     * 分页获取动态
     * GET /api/activities/timeline?page=0&size=10
     */
    @GetMapping("/timeline")
    public ResponseEntity<Map<String, Object>> getActivityTimeline(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        
        try {
            Long coupleId = jwtUtil.getCoupleIdFromRequest(request);
            if (coupleId == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("未找到情侣关系"));
            }
            
            Page<Activity> activitiesPage = activityService.getRecentActivities(coupleId, page, size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", activitiesPage.getContent());
            response.put("totalElements", activitiesPage.getTotalElements());
            response.put("totalPages", activitiesPage.getTotalPages());
            response.put("currentPage", page);
            response.put("pageSize", size);
            response.put("message", "获取动态时间线成功");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("获取动态时间线失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取今天的动态
     * GET /api/activities/today
     */
    @GetMapping("/today")
    public ResponseEntity<Map<String, Object>> getTodayActivities(HttpServletRequest request) {
        try {
            Long coupleId = jwtUtil.getCoupleIdFromRequest(request);
            if (coupleId == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("未找到情侣关系"));
            }
            
            List<Activity> activities = activityService.getTodayActivities(coupleId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", activities);
            response.put("total", activities.size());
            response.put("message", "获取今天动态成功");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("获取今天动态失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取本周动态
     * GET /api/activities/weekly
     */
    @GetMapping("/weekly")
    public ResponseEntity<Map<String, Object>> getWeeklyActivities(HttpServletRequest request) {
        try {
            Long coupleId = jwtUtil.getCoupleIdFromRequest(request);
            if (coupleId == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("未找到情侣关系"));
            }
            
            List<Activity> activities = activityService.getWeeklyActivities(coupleId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", activities);
            response.put("total", activities.size());
            response.put("message", "获取本周动态成功");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("获取本周动态失败: " + e.getMessage()));
        }
    }
    
    /**
     * 根据类型获取动态
     * GET /api/activities/type/{activityType}
     */
    @GetMapping("/type/{activityType}")
    public ResponseEntity<Map<String, Object>> getActivitiesByType(
            @PathVariable String activityType,
            HttpServletRequest request) {
        
        try {
            Long coupleId = jwtUtil.getCoupleIdFromRequest(request);
            if (coupleId == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("未找到情侣关系"));
            }
            
            Activity.ActivityType type = Activity.ActivityType.valueOf(activityType.toUpperCase());
            List<Activity> activities = activityService.getActivitiesByType(coupleId, type);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", activities);
            response.put("total", activities.size());
            response.put("activityType", type.getDisplayName());
            response.put("message", "获取" + type.getDisplayName() + "动态成功");
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(createErrorResponse("无效的动态类型: " + activityType));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("获取动态失败: " + e.getMessage()));
        }
    }
    
    /**
     * 根据用户获取动态
     * GET /api/activities/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getActivitiesByUser(
            @PathVariable Long userId,
            HttpServletRequest request) {
        
        try {
            Long coupleId = jwtUtil.getCoupleIdFromRequest(request);
            if (coupleId == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("未找到情侣关系"));
            }
            
            List<Activity> activities = activityService.getActivitiesByUser(coupleId, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", activities);
            response.put("total", activities.size());
            response.put("userId", userId);
            response.put("message", "获取用户动态成功");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("获取用户动态失败: " + e.getMessage()));
        }
    }
    
    /**
     * 根据时间范围获取动态
     * GET /api/activities/range?startDate=2024-01-01T00:00:00&endDate=2024-01-31T23:59:59
     */
    @GetMapping("/range")
    public ResponseEntity<Map<String, Object>> getActivitiesByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate,
            HttpServletRequest request) {
        
        try {
            Long coupleId = jwtUtil.getCoupleIdFromRequest(request);
            if (coupleId == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("未找到情侣关系"));
            }
            
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            LocalDateTime start = LocalDateTime.parse(startDate, formatter);
            LocalDateTime end = LocalDateTime.parse(endDate, formatter);
            
            List<Activity> activities = activityService.getActivitiesByDateRange(coupleId, start, end);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", activities);
            response.put("total", activities.size());
            response.put("startDate", startDate);
            response.put("endDate", endDate);
            response.put("message", "获取时间范围内动态成功");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("获取动态失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取动态统计信息
     * GET /api/activities/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getActivityStatistics(HttpServletRequest request) {
        try {
            Long coupleId = jwtUtil.getCoupleIdFromRequest(request);
            if (coupleId == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("未找到情侣关系"));
            }
            
            Map<String, Object> statistics = activityService.getActivityStatistics(coupleId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", statistics);
            response.put("message", "获取动态统计成功");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("获取动态统计失败: " + e.getMessage()));
        }
    }
    
    /**
     * 删除动态
     * DELETE /api/activities/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteActivity(
            @PathVariable Long id,
            HttpServletRequest request) {
        
        try {
            Long coupleId = jwtUtil.getCoupleIdFromRequest(request);
            if (coupleId == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("未找到情侣关系"));
            }
            
            boolean deleted = activityService.deleteActivity(id, coupleId);
            
            if (deleted) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "删除动态成功");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(createErrorResponse("动态不存在或已被删除"));
            }
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("删除动态失败: " + e.getMessage()));
        }
    }
    
    /**
     * 手动创建动态（用于测试）
     * POST /api/activities
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createActivity(
            @RequestBody Map<String, Object> requestBody,
            HttpServletRequest request) {
        
        try {
            Long coupleId = jwtUtil.getCoupleIdFromRequest(request);
            String token = jwtUtil.getTokenFromRequest(request);
            Long userId = jwtUtil.getUserIdFromToken(token);
            
            if (coupleId == null || userId == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("未找到用户或情侣关系"));
            }
            
            String activityTypeStr = (String) requestBody.get("activityType");
            String title = (String) requestBody.get("title");
            String description = (String) requestBody.get("description");
            Long referenceId = requestBody.get("referenceId") != null ? 
                    Long.valueOf(requestBody.get("referenceId").toString()) : null;
            String referenceType = (String) requestBody.get("referenceType");
            
            Activity.ActivityType activityType = Activity.ActivityType.valueOf(activityTypeStr.toUpperCase());
            
            Activity activity = activityService.createActivity(coupleId, userId, activityType, title, description, referenceId, referenceType);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", activity);
            response.put("message", "创建动态成功");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("创建动态失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取所有动态类型
     * GET /api/activities/types
     */
    @GetMapping("/types")
    public ResponseEntity<Map<String, Object>> getActivityTypes() {
        try {
            Map<String, String> types = new HashMap<>();
            for (Activity.ActivityType type : Activity.ActivityType.values()) {
                types.put(type.name(), type.getDisplayName());
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", types);
            response.put("message", "获取动态类型成功");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("获取动态类型失败: " + e.getMessage()));
        }
    }
    
    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}