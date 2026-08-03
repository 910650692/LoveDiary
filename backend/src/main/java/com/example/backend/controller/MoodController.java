package com.example.backend.controller;

import com.example.backend.model.MoodRecord;
import com.example.backend.service.MoodService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/moods")
public class MoodController {

    @Autowired
    private MoodService moodService;

    /**
     * 创建或更新今日情绪记录
     * POST /api/moods
     */
    @PostMapping
    public Map<String, Object> recordMood(@RequestBody MoodRecordRequest request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");

        MoodRecord mood = moodService.recordMood(
                userId,
                request.getMoodType(),
                request.getMoodLevel(),
                request.getReason()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", mood);
        return response;
    }

    /**
     * 获取我的情绪记录
     * GET /api/moods/my?startDate=2024-12-01&endDate=2024-12-31
     */
    @GetMapping("/my")
    public Map<String, Object> getMyMoods(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");
        List<MoodRecord> moods = moodService.getMyMoods(userId, startDate, endDate);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", moods);
        return response;
    }

    /**
     * 获取对方的情绪记录
     * GET /api/moods/partner?startDate=2024-12-01&endDate=2024-12-31
     */
    @GetMapping("/partner")
    public Map<String, Object> getPartnerMoods(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");
        List<MoodRecord> moods = moodService.getPartnerMoods(userId, startDate, endDate);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", moods);
        return response;
    }

    /**
     * 获取今日情侣双方情绪
     * GET /api/moods/today
     */
    @GetMapping("/today")
    public Map<String, Object> getTodayMood(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Map<String, MoodRecord> moods = moodService.getTodayMood(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", moods);
        return response;
    }

    /**
     * 获取某天的详细情绪记录
     * GET /api/moods/{moodId}
     */
    @GetMapping("/{moodId}")
    public Map<String, Object> getMoodById(@PathVariable Long moodId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        MoodRecord mood = moodService.getMoodById(userId, moodId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", mood);
        return response;
    }

    /**
     * 删除情绪记录
     * DELETE /api/moods/{moodId}
     */
    @DeleteMapping("/{moodId}")
    public Map<String, Object> deleteMood(@PathVariable Long moodId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        moodService.deleteMood(userId, moodId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "删除成功");
        return response;
    }

    /**
     * 获取日历视图数据（某月的情绪记录）
     * GET /api/moods/calendar?year=2024&month=12
     */
    @GetMapping("/calendar")
    public Map<String, Object> getMonthlyCalendar(
            @RequestParam int year,
            @RequestParam int month,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");
        Map<String, Map<LocalDate, MoodRecord>> calendar = moodService.getMonthlyCalendar(userId, year, month);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", calendar);
        return response;
    }

    /**
     * 请求体DTO
     */
    public static class MoodRecordRequest {
        private MoodRecord.MoodType moodType;
        private Integer moodLevel;
        private String reason;

        public MoodRecord.MoodType getMoodType() {
            return moodType;
        }

        public void setMoodType(MoodRecord.MoodType moodType) {
            this.moodType = moodType;
        }

        public Integer getMoodLevel() {
            return moodLevel;
        }

        public void setMoodLevel(Integer moodLevel) {
            this.moodLevel = moodLevel;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
