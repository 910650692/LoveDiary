package com.example.backend.service;

import com.example.backend.model.Couple;
import com.example.backend.model.MoodRecord;
import com.example.backend.model.User;
import com.example.backend.respository.CoupleRepository;
import com.example.backend.respository.MoodRepository;
import com.example.backend.respository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class MoodService {

    @Autowired
    private MoodRepository moodRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoupleRepository coupleRepository;

    /**
     * 创建或更新今日情绪记录
     */
    public MoodRecord recordMood(Long userId, MoodRecord.MoodType moodType, Integer moodLevel, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getCoupleId() == null) {
            throw new RuntimeException("您还没有匹配情侣");
        }

        LocalDate today = LocalDate.now();
        Optional<MoodRecord> existingRecord = moodRepository
                .findByUserIdAndRecordDateAndIsDeletedFalse(userId, today);

        MoodRecord record;
        if (existingRecord.isPresent()) {
            record = existingRecord.get();
            record.setMoodType(moodType);
            record.setMoodLevel(moodLevel);
            record.setReason(reason);
        } else {
            record = new MoodRecord();
            record.setUserId(userId);
            record.setCoupleId(user.getCoupleId());
            record.setMoodType(moodType);
            record.setMoodLevel(moodLevel);
            record.setReason(reason);
            record.setRecordDate(today);
        }

        return moodRepository.save(record);
    }

    /**
     * 获取我的情绪记录（按日期范围）
     */
    public List<MoodRecord> getMyMoods(Long userId, LocalDate startDate, LocalDate endDate) {
        return moodRepository.findByUserIdAndRecordDateBetweenAndIsDeletedFalseOrderByRecordDateDesc(
                userId, startDate, endDate);
    }

    /**
     * 获取对方的情绪记录
     */
    public List<MoodRecord> getPartnerMoods(Long userId, LocalDate startDate, LocalDate endDate) {
        Long partnerId = getPartnerId(userId);
        return moodRepository.findByUserIdAndRecordDateBetweenAndIsDeletedFalseOrderByRecordDateDesc(
                partnerId, startDate, endDate);
    }

    /**
     * 获取今日情侣双方情绪
     */
    public Map<String, MoodRecord> getTodayMood(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getCoupleId() == null) {
            throw new RuntimeException("您还没有匹配情侣");
        }

        Long partnerId = getPartnerId(userId);
        LocalDate today = LocalDate.now();

        Map<String, MoodRecord> result = new HashMap<>();

        moodRepository.findByUserIdAndRecordDateAndIsDeletedFalse(userId, today)
                .ifPresent(mood -> result.put("myMood", mood));

        moodRepository.findByUserIdAndRecordDateAndIsDeletedFalse(partnerId, today)
                .ifPresent(mood -> result.put("partnerMood", mood));

        return result;
    }

    /**
     * 根据ID获取情绪记录
     */
    public MoodRecord getMoodById(Long userId, Long moodId) {
        MoodRecord mood = moodRepository.findByIdAndIsDeletedFalse(moodId)
                .orElseThrow(() -> new RuntimeException("情绪记录不存在"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!mood.getCoupleId().equals(user.getCoupleId())) {
            throw new RuntimeException("无权访问此记录");
        }

        return mood;
    }

    /**
     * 删除情绪记录（软删除）
     */
    public void deleteMood(Long userId, Long moodId) {
        MoodRecord mood = getMoodById(userId, moodId);

        if (!mood.getUserId().equals(userId)) {
            throw new RuntimeException("只能删除自己的情绪记录");
        }

        mood.setIsDeleted(true);
        moodRepository.save(mood);
    }

    /**
     * 获取情侣双方某月的情绪记录（日历视图）
     */
    public Map<String, Map<LocalDate, MoodRecord>> getMonthlyCalendar(Long userId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        Long partnerId = getPartnerId(userId);

        List<MoodRecord> myMoods = moodRepository
                .findByUserIdAndRecordDateBetweenAndIsDeletedFalseOrderByRecordDateDesc(
                        userId, startDate, endDate);

        List<MoodRecord> partnerMoods = moodRepository
                .findByUserIdAndRecordDateBetweenAndIsDeletedFalseOrderByRecordDateDesc(
                        partnerId, startDate, endDate);

        Map<String, Map<LocalDate, MoodRecord>> result = new HashMap<>();
        result.put("myMoods", convertToDateMap(myMoods));
        result.put("partnerMoods", convertToDateMap(partnerMoods));

        return result;
    }

    /**
     * 获取对方用户ID
     */
    private Long getPartnerId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getCoupleId() == null) {
            throw new RuntimeException("您还没有匹配情侣");
        }

        Couple couple = coupleRepository.findById(user.getCoupleId())
                .orElseThrow(() -> new RuntimeException("情侣关系不存在"));

        return couple.getUser1Id().equals(userId) ? couple.getUser2Id() : couple.getUser1Id();
    }

    /**
     * 转换为日期Map
     */
    private Map<LocalDate, MoodRecord> convertToDateMap(List<MoodRecord> records) {
        Map<LocalDate, MoodRecord> map = new HashMap<>();
        for (MoodRecord record : records) {
            map.put(record.getRecordDate(), record);
        }
        return map;
    }
}
