package com.example.backend.controller;

import com.example.backend.model.PeriodRecord;
import com.example.backend.service.PeriodRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 生理期记录控制器
 * 提供生理期记录管理和预测功能的REST API接口
 */
@RestController
@RequestMapping("/api/period-records")
public class PeriodRecordController {
    
    @Autowired
    private PeriodRecordService periodRecordService;
    
    /**
     * 创建生理期记录
     * POST /api/period-records
     */
    @PostMapping
    public ResponseEntity<?> createRecord(@RequestBody CreateRecordRequest request, HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            
            PeriodRecord record = new PeriodRecord();
            record.setStartDate(request.getStartDate());
            record.setEndDate(request.getEndDate());
            
            PeriodRecord savedRecord = periodRecordService.createRecord(userId, record);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "生理期记录创建成功",
                    "data", savedRecord
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 更新生理期记录
     * PUT /api/period-records/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateRecord(@PathVariable Long id, 
                                        @RequestBody UpdateRecordRequest request, 
                                        HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            
            PeriodRecord record = new PeriodRecord();
            record.setStartDate(request.getStartDate());
            record.setEndDate(request.getEndDate());
            
            PeriodRecord updatedRecord = periodRecordService.updateRecord(userId, id, record);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "生理期记录更新成功",
                    "data", updatedRecord
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 删除生理期记录
     * DELETE /api/period-records/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRecord(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            
            periodRecordService.deleteRecord(userId, id);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "生理期记录删除成功"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 获取生理期记录列表
     * GET /api/period-records
     */
    @GetMapping
    public ResponseEntity<?> getRecords(@RequestParam(required = false) String startDate,
                                       @RequestParam(required = false) String endDate,
                                       @RequestParam(required = false) Boolean isPredicted,
                                       HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            
            LocalDate start = startDate != null ? LocalDate.parse(startDate) : null;
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : null;
            
            List<PeriodRecord> records = periodRecordService.getUserRecords(userId, start, end, isPredicted);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", records,
                    "count", records.size()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 获取生理期预测信息
     * GET /api/period-records/prediction
     */
    @GetMapping("/prediction")
    public ResponseEntity<?> getPrediction(HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            
            Map<String, Object> prediction = periodRecordService.getPrediction(userId);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", prediction
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 手动生成预测记录
     * POST /api/period-records/generate-predictions
     */
    @PostMapping("/generate-predictions")
    public ResponseEntity<?> generatePredictions(HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            
            periodRecordService.generatePredictions(userId);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "预测记录生成成功"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 获取用户统计信息
     * GET /api/period-records/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics(HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            
            // 获取最近的记录进行统计分析
            List<PeriodRecord> actualRecords = periodRecordService.getUserRecords(userId, null, null, false);
            List<PeriodRecord> predictedRecords = periodRecordService.getUserRecords(userId, null, null, true);
            
            Map<String, Object> statistics = Map.of(
                    "totalActualRecords", actualRecords.size(),
                    "totalPredictedRecords", predictedRecords.size(),
                    "canPredict", actualRecords.size() >= 2,
                    "lastRecordDate", actualRecords.isEmpty() ? null : actualRecords.get(0).getStartDate()
            );
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", statistics
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 创建记录请求DTO
     */
    public static class CreateRecordRequest {
        private LocalDate startDate;
        private LocalDate endDate;
        
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    }
    
    /**
     * 更新记录请求DTO
     */
    public static class UpdateRecordRequest {
        private LocalDate startDate;
        private LocalDate endDate;
        
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    }
}