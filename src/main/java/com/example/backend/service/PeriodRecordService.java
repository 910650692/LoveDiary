package com.example.backend.service;

import com.example.backend.model.PeriodRecord;
import com.example.backend.model.Couple;
import com.example.backend.respository.PeriodRecordRepository;
import com.example.backend.respository.CoupleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 生理期记录服务类
 * 处理生理期记录的业务逻辑，包括CRUD操作和智能预测功能
 */
@Service
@Transactional
public class PeriodRecordService {
    
    @Autowired
    private PeriodRecordRepository periodRecordRepository;
    
    @Autowired
    private CoupleRepository coupleRepository;
    
    /**
     * 创建生理期记录
     */
    public PeriodRecord createRecord(Long userId, PeriodRecord record) {
        validateUserAccess(userId);
        
        record.setUserId(userId);
        record.setCoupleId(getUserCoupleId(userId));
        record.setIsPredicted(false);
        
        if (!record.isValid()) {
            throw new IllegalArgumentException("生理期记录数据无效");
        }
        
        checkForConflicts(record);
        
        calculateCycleLength(record);
        
        PeriodRecord savedRecord = periodRecordRepository.save(record);
        
        if (record.isEnded()) {
            generatePredictions(userId);
        }
        
        return savedRecord;
    }
    
    /**
     * 更新生理期记录
     */
    public PeriodRecord updateRecord(Long userId, Long recordId, PeriodRecord updatedRecord) {
        PeriodRecord existingRecord = periodRecordRepository.findByIdAndUserIdAndIsDeletedFalse(recordId, userId)
                .orElseThrow(() -> new IllegalArgumentException("记录不存在或无权限访问"));
        
        existingRecord.setStartDate(updatedRecord.getStartDate());
        existingRecord.setEndDate(updatedRecord.getEndDate());
        
        if (!existingRecord.isValid()) {
            throw new IllegalArgumentException("更新后的记录数据无效");
        }
        
        calculateCycleLength(existingRecord);
        
        PeriodRecord savedRecord = periodRecordRepository.save(existingRecord);
        
        if (existingRecord.isEnded() && existingRecord.isActual()) {
            generatePredictions(userId);
        }
        
        return savedRecord;
    }
    
    /**
     * 删除生理期记录（软删除）
     */
    public void deleteRecord(Long userId, Long recordId) {
        PeriodRecord record = periodRecordRepository.findByIdAndUserIdAndIsDeletedFalse(recordId, userId)
                .orElseThrow(() -> new IllegalArgumentException("记录不存在或无权限访问"));
        
        record.setIsDeleted(true);
        periodRecordRepository.save(record);
        
        if (record.isActual()) {
            generatePredictions(userId);
        }
    }
    
    /**
     * 获取用户的生理期记录列表
     */
    public List<PeriodRecord> getUserRecords(Long userId, LocalDate startDate, LocalDate endDate, Boolean isPredicted) {
        validateUserAccess(userId);
        
        if (startDate != null && endDate != null) {
            List<PeriodRecord> records = periodRecordRepository
                    .findByUserIdAndStartDateBetweenAndIsDeletedFalseOrderByStartDateDesc(userId, startDate, endDate);
            
            if (isPredicted != null) {
                return records.stream()
                        .filter(r -> r.getIsPredicted().equals(isPredicted))
                        .collect(Collectors.toList());
            }
            return records;
        }
        
        if (isPredicted != null) {
            if (isPredicted) {
                return periodRecordRepository.findByUserIdAndIsPredictedTrueAndIsDeletedFalseOrderByStartDateDesc(userId);
            } else {
                return periodRecordRepository.findByUserIdAndIsPredictedFalseAndIsDeletedFalseOrderByStartDateDesc(userId);
            }
        }
        
        return periodRecordRepository.findByUserIdAndIsDeletedFalseOrderByStartDateDesc(userId);
    }
    
    /**
     * 获取生理期预测信息
     */
    public Map<String, Object> getPrediction(Long userId) {
        validateUserAccess(userId);
        
        List<PeriodRecord> recentRecords = periodRecordRepository.findRecentCompletedRecords(userId, 6);
        
        if (recentRecords.size() < 2) {
            return Map.of(
                    "canPredict", false,
                    "message", "需要至少2个完整的生理期记录才能进行预测"
            );
        }
        
        PredictionResult prediction = calculatePrediction(recentRecords);
        
        return Map.of(
                "canPredict", true,
                "nextStartDate", prediction.getNextStartDate(),
                "predictedStartRange", Map.of(
                        "earliest", prediction.getNextStartDate().minusDays(prediction.getUncertaintyDays()),
                        "latest", prediction.getNextStartDate().plusDays(prediction.getUncertaintyDays())
                ),
                "averageCycleLength", prediction.getAverageCycleLength(),
                "cycleRegularity", prediction.getRegularity(),
                "basedOnCycles", recentRecords.size() - 1
        );
    }
    
    /**
     * 生成预测记录
     */
    public void generatePredictions(Long userId) {
        clearExistingPredictions(userId);
        
        List<PeriodRecord> recentRecords = periodRecordRepository.findRecentCompletedRecords(userId, 6);
        
        if (recentRecords.size() < 2) {
            return;
        }
        
        PredictionResult prediction = calculatePrediction(recentRecords);
        
        LocalDate nextStartDate = prediction.getNextStartDate();
        Long coupleId = getUserCoupleId(userId);
        
        for (int i = 1; i <= 3; i++) {
            PeriodRecord predictedRecord = new PeriodRecord();
            predictedRecord.setUserId(userId);
            predictedRecord.setCoupleId(coupleId);
            predictedRecord.setStartDate(nextStartDate);
            predictedRecord.setEndDate(nextStartDate.plusDays(prediction.getAveragePeriodLength() - 1));
            predictedRecord.setIsPredicted(true);
            predictedRecord.setCycleLength(prediction.getAverageCycleLength());
            
            periodRecordRepository.save(predictedRecord);
            
            nextStartDate = nextStartDate.plusDays(prediction.getAverageCycleLength());
        }
    }
    
    /**
     * 计算周期长度
     */
    private void calculateCycleLength(PeriodRecord record) {
        Optional<PeriodRecord> previousRecord = findPreviousActualRecord(record.getUserId(), record.getStartDate());
        
        if (previousRecord.isPresent()) {
            Integer cycleLength = record.calculateCycleLengthFrom(previousRecord.get());
            record.setCycleLength(cycleLength);
        }
    }
    
    /**
     * 查找上一个实际记录
     */
    private Optional<PeriodRecord> findPreviousActualRecord(Long userId, LocalDate currentStartDate) {
        return periodRecordRepository.findByUserIdAndIsPredictedFalseAndIsDeletedFalseOrderByStartDateDesc(userId)
                .stream()
                .filter(r -> r.getStartDate().isBefore(currentStartDate))
                .findFirst();
    }
    
    /**
     * 计算预测结果
     */
    private PredictionResult calculatePrediction(List<PeriodRecord> recentRecords) {
        List<Integer> cycleLengths = new ArrayList<>();
        List<Integer> periodLengths = new ArrayList<>();
        
        for (int i = 0; i < recentRecords.size() - 1; i++) {
            PeriodRecord current = recentRecords.get(i);
            PeriodRecord previous = recentRecords.get(i + 1);
            
            int cycleLength = (int) ChronoUnit.DAYS.between(previous.getStartDate(), current.getStartDate());
            cycleLengths.add(cycleLength);
            
            if (current.getDuration() != null) {
                periodLengths.add(current.getDuration());
            }
        }
        
        int avgCycleLength = (int) cycleLengths.stream().mapToInt(Integer::intValue).average().orElse(28);
        int avgPeriodLength = (int) periodLengths.stream().mapToInt(Integer::intValue).average().orElse(5);
        
        double cycleStdDev = calculateStandardDeviation(cycleLengths);
        String regularity = cycleStdDev <= 2 ? "规律" : cycleStdDev <= 4 ? "较规律" : "不规律";
        int uncertaintyDays = (int) Math.ceil(cycleStdDev);
        
        LocalDate lastStartDate = recentRecords.get(0).getStartDate();
        LocalDate nextStartDate = lastStartDate.plusDays(avgCycleLength);
        
        return new PredictionResult(nextStartDate, avgCycleLength, avgPeriodLength, regularity, uncertaintyDays);
    }
    
    /**
     * 计算标准差
     */
    private double calculateStandardDeviation(List<Integer> values) {
        if (values.isEmpty()) return 0;
        
        double mean = values.stream().mapToInt(Integer::intValue).average().orElse(0);
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0);
        
        return Math.sqrt(variance);
    }
    
    /**
     * 清理现有预测记录
     */
    private void clearExistingPredictions(Long userId) {
        List<PeriodRecord> predictions = periodRecordRepository
                .findByUserIdAndIsPredictedTrueAndIsDeletedFalseOrderByStartDateDesc(userId);
        
        predictions.forEach(p -> {
            p.setIsDeleted(true);
            periodRecordRepository.save(p);
        });
    }
    
    /**
     * 检查记录冲突
     */
    private void checkForConflicts(PeriodRecord record) {
        LocalDate checkStart = record.getStartDate().minusDays(7);
        LocalDate checkEnd = record.getStartDate().plusDays(7);
        
        List<PeriodRecord> conflictingRecords = periodRecordRepository
                .findByUserIdAndStartDateBetweenAndIsDeletedFalse(record.getUserId(), checkStart, checkEnd);
        
        if (!conflictingRecords.isEmpty()) {
            throw new IllegalArgumentException("该日期附近已存在生理期记录");
        }
    }
    
    /**
     * 验证用户访问权限
     */
    private void validateUserAccess(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
    }
    
    /**
     * 获取用户的情侣ID
     */
    private Long getUserCoupleId(Long userId) {
        return coupleRepository.findByUserId(userId)
                .map(Couple::getId)
                .orElseThrow(() -> new IllegalArgumentException("用户未建立情侣关系"));
    }
    
    /**
     * 预测结果内部类
     */
    private static class PredictionResult {
        private final LocalDate nextStartDate;
        private final Integer averageCycleLength;
        private final Integer averagePeriodLength;
        private final String regularity;
        private final Integer uncertaintyDays;
        
        public PredictionResult(LocalDate nextStartDate, Integer averageCycleLength, 
                              Integer averagePeriodLength, String regularity, Integer uncertaintyDays) {
            this.nextStartDate = nextStartDate;
            this.averageCycleLength = averageCycleLength;
            this.averagePeriodLength = averagePeriodLength;
            this.regularity = regularity;
            this.uncertaintyDays = uncertaintyDays;
        }
        
        public LocalDate getNextStartDate() { return nextStartDate; }
        public Integer getAverageCycleLength() { return averageCycleLength; }
        public Integer getAveragePeriodLength() { return averagePeriodLength; }
        public String getRegularity() { return regularity; }
        public Integer getUncertaintyDays() { return uncertaintyDays; }
    }
}