package com.example.backend.controller;

import com.example.backend.model.Anniversary;
import com.example.backend.service.AnniversaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 纪念日控制器 - 简单版本
 * 这里定义API接口，处理HTTP请求
 */
@RestController  // 标记这是REST API控制器
@RequestMapping("/api/anniversaries")  // 所有方法的URL都以这个开头
public class AnniversaryController {
    
    @Autowired  // 注入Service
    private AnniversaryService anniversaryService;
    
    /**
     * 获取所有纪念日
     * GET /api/anniversaries
     */
    @GetMapping
    public List<Anniversary> getAllAnniversaries() {
        return anniversaryService.getAllAnniversaries();
    }
    
    /**
     * 根据情侣ID获取纪念日
     * GET /api/anniversaries/couple/1
     */
    @GetMapping("/couple/{coupleId}")
    public List<Anniversary> getAnniversariesByCoupleId(@PathVariable Long coupleId) {
        return anniversaryService.getAnniversariesByCoupleId(coupleId);
    }
    
    /**
     * 根据ID获取单个纪念日
     * GET /api/anniversaries/1
     */
    @GetMapping("/{id}")
    public Optional<Anniversary> getAnniversaryById(@PathVariable Long id) {
        return anniversaryService.getAnniversaryById(id);
    }
    
    /**
     * 创建新纪念日
     * POST /api/anniversaries
     */
    @PostMapping
    public Anniversary createAnniversary(@RequestBody Anniversary anniversary) {
        return anniversaryService.saveAnniversary(anniversary);
    }
    
    /**
     * 更新纪念日
     * PUT /api/anniversaries/1
     */
    @PutMapping("/{id}")
    public Anniversary updateAnniversary(@PathVariable Long id, @RequestBody Anniversary anniversary) {
        anniversary.setId(id);  // 确保使用URL中的ID
        return anniversaryService.saveAnniversary(anniversary);
    }
    
    /**
     * 删除纪念日
     * DELETE /api/anniversaries/1
     */
    @DeleteMapping("/{id}")
    public String deleteAnniversary(@PathVariable Long id) {
        anniversaryService.deleteAnniversary(id);
        return "删除成功";
    }
    
    /**
     * 搜索纪念日
     * GET /api/anniversaries/search?keyword=生日
     */
    @GetMapping("/search")
    public List<Anniversary> searchAnniversaries(@RequestParam String keyword) {
        return anniversaryService.searchAnniversaries(keyword);
    }
    
    // ========== 新增：纪念日计算相关的API接口 ==========
    
    /**
     * 获取即将到来的纪念日（默认30天内）
     * GET /api/anniversaries/couple/1/upcoming
     */
    @GetMapping("/couple/{coupleId}/upcoming")
    public List<Anniversary> getUpcomingAnniversaries(@PathVariable Long coupleId) {
        return anniversaryService.getUpcomingAnniversaries(coupleId);
    }
    
    /**
     * 获取即将到来的纪念日（指定天数内）
     * GET /api/anniversaries/couple/1/upcoming/7
     * 特殊用法：days=0 表示获取今天的纪念日
     */
    @GetMapping("/couple/{coupleId}/upcoming/{days}")
    public List<Anniversary> getUpcomingAnniversaries(@PathVariable Long coupleId, @PathVariable int days) {
        return anniversaryService.getUpcomingAnniversaries(coupleId, days);
    }
    
    /**
     * 获取纪念日概览（今天+即将到来的）
     * GET /api/anniversaries/couple/1/overview
     */
    @GetMapping("/couple/{coupleId}/overview")
    public Map<String, Object> getAnniversaryOverview(@PathVariable Long coupleId) {
        List<Anniversary> today = anniversaryService.getUpcomingAnniversaries(coupleId, 0);
        List<Anniversary> upcoming7Days = anniversaryService.getUpcomingAnniversaries(coupleId, 7);
        List<Anniversary> upcoming30Days = anniversaryService.getUpcomingAnniversaries(coupleId, 30);
        
        Map<String, Object> overview = new java.util.HashMap<>();
        overview.put("today", today);
        overview.put("hasToday", !today.isEmpty());
        overview.put("next7Days", upcoming7Days);
        overview.put("next30Days", upcoming30Days);
        overview.put("thisMonth", anniversaryService.getThisMonthAnniversaries(coupleId));
        
        return overview;
    }
    
    /**
     * 获取纪念日的详细统计信息
     * GET /api/anniversaries/1/stats
     */
    @GetMapping("/{id}/stats")
    public Map<String, Object> getAnniversaryStats(@PathVariable Long id) {
        Map<String, Object> stats = anniversaryService.getAnniversaryStats(id);
        if (stats == null) {
            throw new RuntimeException("纪念日不存在，ID: " + id);
        }
        return stats;
    }
    
    /**
     * 检查是否有重要纪念日即将到来（7天内）
     * GET /api/anniversaries/couple/1/important-coming
     */
    @GetMapping("/couple/{coupleId}/important-coming")
    public Map<String, Object> checkImportantAnniversaryComingUp(@PathVariable Long coupleId) {
        boolean hasImportant = anniversaryService.hasImportantAnniversaryComingUp(coupleId);
        List<Anniversary> upcoming = anniversaryService.getUpcomingAnniversaries(coupleId, 7);
        
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("hasImportantAnniversary", hasImportant);
        result.put("upcomingAnniversaries", upcoming);
        result.put("count", upcoming.size());
        
        return result;
    }
    
    // ========== 新增：iOS推送相关API ==========
    
    /**
     * 获取推送通知数据（为iOS本地推送提供内容）
     * GET /api/anniversaries/couple/1/notifications
     */
    @GetMapping("/couple/{coupleId}/notifications")
    public List<Map<String, Object>> getNotificationData(@PathVariable Long coupleId) {
        return anniversaryService.getNotificationData(coupleId);
    }
    
    /**
     * 获取单个纪念日的推送内容
     * GET /api/anniversaries/1/notification-content
     */
    @GetMapping("/{id}/notification-content")
    public Map<String, Object> getNotificationContent(@PathVariable Long id) {
        Map<String, Object> content = anniversaryService.getNotificationContent(id);
        if (content == null) {
            throw new RuntimeException("纪念日不存在，ID: " + id);
        }
        return content;
    }
}
