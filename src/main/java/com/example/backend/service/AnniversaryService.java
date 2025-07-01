package com.example.backend.service;

import com.example.backend.model.Anniversary;
import com.example.backend.respository.AnniversaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 纪念日业务逻辑类 - 简单版本
 * 这里写我们的业务逻辑，调用Repository操作数据库
 */
@Service  // 告诉Spring这是一个服务类
public class AnniversaryService {
    
    @Autowired  // 自动注入Repository
    private AnniversaryRepository anniversaryRepository;
    
    /**
     * 获取所有纪念日
     */
    public List<Anniversary> getAllAnniversaries() {
        return anniversaryRepository.findAll();
    }
    
    /**
     * 根据情侣ID获取纪念日
     */
    public List<Anniversary> getAnniversariesByCoupleId(Long coupleId) {
        return anniversaryRepository.findByCoupleId(coupleId);
    }
    
    /**
     * 根据ID获取单个纪念日
     */
    public Optional<Anniversary> getAnniversaryById(Long id) {
        return anniversaryRepository.findById(id);
    }
    
    /**
     * 保存纪念日（新增或更新）
     */
    public Anniversary saveAnniversary(Anniversary anniversary) {
        return anniversaryRepository.save(anniversary);
    }
    
    /**
     * 删除纪念日
     */
    public void deleteAnniversary(Long id) {
        anniversaryRepository.deleteById(id);
    }
    
    /**
     * 搜索纪念日
     */
    public List<Anniversary> searchAnniversaries(String keyword) {
        return anniversaryRepository.findByNameContaining(keyword);
    }
    
    // ========== 新增：纪念日计算相关的业务方法 ==========
    
    /**
     * 获取即将到来的纪念日（未来N天内）
     * 
     * @param coupleId 情侣ID
     * @param days 未来多少天内，默认30天
     * @return 即将到来的纪念日列表，按距离排序
     */
    public List<Anniversary> getUpcomingAnniversaries(Long coupleId, int days) {
        List<Anniversary> allAnniversaries = anniversaryRepository.findByCoupleId(coupleId);
        
        return allAnniversaries.stream()
                .filter(anniversary -> anniversary.getDaysUntilNext() <= days)
                .sorted((a1, a2) -> Long.compare(a1.getDaysUntilNext(), a2.getDaysUntilNext()))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取即将到来的纪念日（默认30天内）
     */
    public List<Anniversary> getUpcomingAnniversaries(Long coupleId) {
        return getUpcomingAnniversaries(coupleId, 30);
    }
    
    /**
     * 获取今天的纪念日
     * 
     * @param coupleId 情侣ID
     * @return 今天的纪念日列表
     */
    public List<Anniversary> getTodayAnniversaries(Long coupleId) {
        List<Anniversary> allAnniversaries = anniversaryRepository.findByCoupleId(coupleId);
        
        return allAnniversaries.stream()
                .filter(Anniversary::isToday)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取本月的纪念日
     * 
     * @param coupleId 情侣ID
     * @return 本月的纪念日列表
     */
    public List<Anniversary> getThisMonthAnniversaries(Long coupleId) {
        List<Anniversary> allAnniversaries = anniversaryRepository.findByCoupleId(coupleId);
        LocalDate today = LocalDate.now();
        int currentMonth = today.getMonthValue();
        
        return allAnniversaries.stream()
                .filter(anniversary -> anniversary.getDate().getMonthValue() == currentMonth)
                .sorted((a1, a2) -> Integer.compare(a1.getDate().getDayOfMonth(), a2.getDate().getDayOfMonth()))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取纪念日统计信息
     * 
     * @param anniversaryId 纪念日ID
     * @return 包含各种统计信息的映射
     */
    public java.util.Map<String, Object> getAnniversaryStats(Long anniversaryId) {
        Optional<Anniversary> anniversaryOpt = anniversaryRepository.findById(anniversaryId);
        
        if (anniversaryOpt.isEmpty()) {
            return null;
        }
        
        Anniversary anniversary = anniversaryOpt.get();
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        
        stats.put("anniversary", anniversary);
        stats.put("daysUntilNext", anniversary.getDaysUntilNext());
        stats.put("daysPassed", anniversary.getDaysPassed());
        stats.put("yearsPassed", anniversary.getYearsPassed());
        stats.put("isToday", anniversary.isToday());
        stats.put("nextAnniversaryDate", anniversary.getNextAnniversaryDate());
        
        return stats;
    }
    
    /**
     * 检查是否有即将到来的重要纪念日（7天内）
     * 
     * @param coupleId 情侣ID
     * @return true表示有重要纪念日即将到来
     */
    public boolean hasImportantAnniversaryComingUp(Long coupleId) {
        List<Anniversary> upcoming = getUpcomingAnniversaries(coupleId, 7);
        return !upcoming.isEmpty();
    }
    
    // ========== 新增：iOS推送相关的业务方法 ==========
    
    /**
     * 获取推送通知数据（为iOS本地推送提供内容）
     * 返回所有纪念日的推送数据，包括当天和提前1天的推送内容
     * 
     * @param coupleId 情侣ID
     * @return 推送通知数据列表
     */
    public List<java.util.Map<String, Object>> getNotificationData(Long coupleId) {
        List<Anniversary> allAnniversaries = anniversaryRepository.findByCoupleId(coupleId);
        List<java.util.Map<String, Object>> notifications = new java.util.ArrayList<>();
        
        for (Anniversary anniversary : allAnniversaries) {
            // 当天推送
            java.util.Map<String, Object> todayNotification = new java.util.HashMap<>();
            todayNotification.put("anniversaryId", anniversary.getId());
            todayNotification.put("type", "today");
            todayNotification.put("title", isBirthdayType(anniversary) ? "🎂 生日快乐！" : "🎉 纪念日快乐！");
            todayNotification.put("body", generateTodayNotificationText(anniversary));
            todayNotification.put("triggerDate", anniversary.getDate().toString());
            todayNotification.put("triggerTime", "08:00");
            todayNotification.put("identifier", "anniversary_" + anniversary.getId() + "_today");
            notifications.add(todayNotification);
            
            // 提前1天推送
            java.util.Map<String, Object> reminderNotification = new java.util.HashMap<>();
            reminderNotification.put("anniversaryId", anniversary.getId());
            reminderNotification.put("type", "reminder");
            reminderNotification.put("title", isBirthdayType(anniversary) ? "🎈 生日提醒" : "⏰ 纪念日提醒");
            reminderNotification.put("body", generateReminderNotificationText(anniversary));
            reminderNotification.put("triggerDate", anniversary.getDate().minusDays(1).toString());
            reminderNotification.put("triggerTime", "20:00");
            reminderNotification.put("identifier", "anniversary_" + anniversary.getId() + "_reminder");
            notifications.add(reminderNotification);
        }
        
        return notifications;
    }
    
    /**
     * 获取单个纪念日的推送内容
     * 
     * @param anniversaryId 纪念日ID
     * @return 推送内容数据
     */
    public java.util.Map<String, Object> getNotificationContent(Long anniversaryId) {
        Optional<Anniversary> anniversaryOpt = anniversaryRepository.findById(anniversaryId);
        
        if (anniversaryOpt.isEmpty()) {
            return null;
        }
        
        Anniversary anniversary = anniversaryOpt.get();
        java.util.Map<String, Object> content = new java.util.HashMap<>();
        
        content.put("anniversary", anniversary);
        content.put("todayContent", generateTodayNotificationText(anniversary));
        content.put("reminderContent", generateReminderNotificationText(anniversary));
        content.put("daysUntilNext", anniversary.getDaysUntilNext());
        content.put("yearsPassed", anniversary.getYearsPassed());
        
        return content;
    }
    
    /**
     * 判断是否是生日类型的纪念日
     */
    private boolean isBirthdayType(Anniversary anniversary) {
        String name = anniversary.getName().toLowerCase();
        return name.contains("生日") || name.contains("birthday");
    }
    
    /**
     * 生成当天推送的文案
     */
    private String generateTodayNotificationText(Anniversary anniversary) {
        if (isBirthdayType(anniversary)) {
            return generateBirthdayNotificationText(anniversary);
        } else {
            return generateAnniversaryNotificationText(anniversary);
        }
    }
    
    /**
     * 生成生日专用的当天推送文案
     */
    private String generateBirthdayNotificationText(Anniversary anniversary) {
        long age = anniversary.getYearsPassed();
        
        String[] birthdayMessages = {
            "生日快乐！愿你被世界温柔以待🎂",
            "又长大了一岁，要更加幸福哦🎉",
            "生日快乐！愿所有美好如期而至🌟",
            "特别的日子，祝你生日快乐💝"
        };
        
        long daysPassed = anniversary.getDaysPassed();
        String randomMessage = birthdayMessages[(int)(daysPassed % birthdayMessages.length)];
        
        if (age > 0) {
            return String.format("🎂 %s\n今年%d岁啦！%s", 
                anniversary.getName(), age, randomMessage);
        } else {
            return String.format("🎂 %s\n%s", 
                anniversary.getName(), randomMessage);
        }
    }
    
    /**
     * 生成普通纪念日的当天推送文案
     */
    private String generateAnniversaryNotificationText(Anniversary anniversary) {
        long yearsPassed = anniversary.getYearsPassed();
        long daysPassed = anniversary.getDaysPassed();
        
        String[] celebrationMessages = {
            "愿你们的爱情长长久久💕",
            "每一天都是最美的纪念🌹",
            "感谢生命中有彼此相伴✨",
            "愿你们永远幸福甜蜜🍯"
        };
        
        String randomMessage = celebrationMessages[(int)(daysPassed % celebrationMessages.length)];
        
        if (yearsPassed > 0) {
            return String.format("今天是你们的\"%s\"纪念日！\n已经陪伴了%d年，%s", 
                anniversary.getName(), yearsPassed, randomMessage);
        } else {
            return String.format("今天是你们的\"%s\"纪念日！\n已经陪伴了%d天，%s", 
                anniversary.getName(), daysPassed, randomMessage);
        }
    }
    
    /**
     * 生成提前1天提醒的文案
     */
    private String generateReminderNotificationText(Anniversary anniversary) {
        if (isBirthdayType(anniversary)) {
            return generateBirthdayReminderText(anniversary);
        } else {
            return generateAnniversaryReminderText(anniversary);
        }
    }
    
    /**
     * 生成生日专用的提前提醒文案
     */
    private String generateBirthdayReminderText(Anniversary anniversary) {
        String[] birthdayReminderMessages = {
            "记得给TA准备生日惊喜哦🎁",
            "要不要订个生日蛋糕呢？🎂", 
            "生日礼物准备好了吗？🎊",
            "明天为TA庆祝生日吧🎈"
        };
        
        long daysPassed = anniversary.getDaysPassed();
        String randomMessage = birthdayReminderMessages[(int)(daysPassed % birthdayReminderMessages.length)];
        
        return String.format("明天是%s\n%s", 
            anniversary.getName(), randomMessage);
    }
    
    /**
     * 生成普通纪念日的提前提醒文案
     */
    private String generateAnniversaryReminderText(Anniversary anniversary) {
        String[] reminderMessages = {
            "记得为TA准备一个小惊喜哦～🎁",
            "要不要计划一个特别的庆祝呢？🎊", 
            "这个特殊的日子值得好好纪念📝",
            "期待你们美好的纪念日时光🌈"
        };
        
        long daysPassed = anniversary.getDaysPassed();
        String randomMessage = reminderMessages[(int)(daysPassed % reminderMessages.length)];
        
        return String.format("明天是你们的\"%s\"纪念日\n%s", 
            anniversary.getName(), randomMessage);
    }
} 