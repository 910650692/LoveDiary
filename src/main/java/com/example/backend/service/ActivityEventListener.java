package com.example.backend.service;

import com.example.backend.model.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.backend.event.TodoCompletedEvent;
import com.example.backend.event.TodoCreatedEvent;
import com.example.backend.event.PhotoUploadedEvent;
import com.example.backend.event.PhotoFavoritedEvent;
import com.example.backend.event.AnniversaryCreatedEvent;
import com.example.backend.event.AnniversaryReminderEvent;
import com.example.backend.event.LoveMilestoneEvent;
import com.example.backend.event.PeriodRecordEvent;
import com.example.backend.event.AlbumCreatedEvent;
import com.example.backend.event.UserJoinedEvent;
import com.example.backend.event.LoveDateSetEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 活动事件监听器
 * 监听系统中的各种业务事件，自动创建对应的用户动态记录
 */
@Component
public class ActivityEventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(ActivityEventListener.class);
    
    @Autowired
    private ActivityService activityService;
    
    /**
     * 监听TodoItem完成事件
     */
    @EventListener
    public void handleTodoCompletedEvent(TodoCompletedEvent event) {
        try {
            String title = "完成了愿望\"" + event.getTodoTitle() + "\"";
            String description = event.getTodoDescription() != null && !event.getTodoDescription().isEmpty() 
                    ? event.getTodoDescription() 
                    : "又完成了一个心愿，真棒！";
            
            activityService.createActivity(
                    event.getCoupleId(),
                    event.getUserId(),
                    Activity.ActivityType.TODO_COMPLETED,
                    title,
                    description,
                    event.getTodoId(),
                    "TodoItem"
            );
        } catch (Exception e) {
            // 记录日志，但不影响主业务流程
            logger.error("Failed to create TODO_COMPLETED activity: {}", e.getMessage());
        }
    }
    
    /**
     * 监听TodoItem创建事件
     */
    @EventListener
    public void handleTodoCreatedEvent(TodoCreatedEvent event) {
        try {
            String title = "添加了新愿望\"" + event.getTodoTitle() + "\"";
            String description = "让我们一起努力实现这个愿望吧！";
            
            activityService.createActivity(
                    event.getCoupleId(),
                    event.getUserId(),
                    Activity.ActivityType.TODO_CREATED,
                    title,
                    description,
                    event.getTodoId(),
                    "TodoItem"
            );
            logger.info("TODO_CREATED activity created successfully: {}", title);
        } catch (Exception e) {
            logger.error("Failed to create TODO_CREATED activity: {}", e.getMessage());
        }
    }
    
    /**
     * 监听照片上传事件
     */
    @EventListener
    public void handlePhotoUploadedEvent(PhotoUploadedEvent event) {
        try {
            String title = event.getPhotoCount() > 1 
                    ? "上传了" + event.getPhotoCount() + "张新照片"
                    : "上传了1张新照片";
            String description = "记录美好的时光";
            
            activityService.createActivity(
                    event.getCoupleId(),
                    event.getUserId(),
                    Activity.ActivityType.PHOTO_UPLOADED,
                    title,
                    description,
                    event.getPhotoId(),
                    "Photo"
            );
        } catch (Exception e) {
            logger.error("Failed to create PHOTO_UPLOADED activity: {}", e.getMessage());
        }
    }
    
    /**
     * 监听照片收藏事件
     */
    @EventListener
    public void handlePhotoFavoritedEvent(PhotoFavoritedEvent event) {
        try {
            String title = "收藏了一张照片";
            String description = "这张照片太美了，值得收藏";
            
            activityService.createActivity(
                    event.getCoupleId(),
                    event.getUserId(),
                    Activity.ActivityType.PHOTO_FAVORITED,
                    title,
                    description,
                    event.getPhotoId(),
                    "Photo"
            );
        } catch (Exception e) {
            logger.error("Failed to create PHOTO_FAVORITED activity: {}", e.getMessage());
        }
    }
    
    /**
     * 监听纪念日创建事件
     */
    @EventListener
    public void handleAnniversaryCreatedEvent(AnniversaryCreatedEvent event) {
        try {
            String title = "创建了纪念日\"" + event.getAnniversaryTitle() + "\"";
            String description = "又多了一个值得纪念的日子";
            
            activityService.createActivity(
                    event.getCoupleId(),
                    event.getUserId(),
                    Activity.ActivityType.ANNIVERSARY_CREATED,
                    title,
                    description,
                    event.getAnniversaryId(),
                    "Anniversary"
            );
        } catch (Exception e) {
            logger.error("Failed to create ANNIVERSARY_CREATED activity: {}", e.getMessage());
        }
    }
    
    /**
     * 监听纪念日提醒事件
     */
    @EventListener
    public void handleAnniversaryReminderEvent(AnniversaryReminderEvent event) {
        try {
            String title = "距离" + event.getAnniversaryTitle() + "还有" + event.getDaysLeft() + "天";
            String description = "记得准备惊喜哦！";
            
            activityService.createActivity(
                    event.getCoupleId(),
                    event.getUserId(),
                    Activity.ActivityType.ANNIVERSARY_REMINDER,
                    title,
                    description,
                    event.getAnniversaryId(),
                    "Anniversary"
            );
        } catch (Exception e) {
            logger.error("Failed to create ANNIVERSARY_REMINDER activity: {}", e.getMessage());
        }
    }
    
    /**
     * 监听恋爱里程碑事件
     */
    @EventListener
    public void handleLoveMilestoneEvent(LoveMilestoneEvent event) {
        try {
            String title = "今天是我们恋爱第" + event.getDays() + "天";
            String description = "时间过得真快，感谢有你相伴";
            
            activityService.createActivity(
                    event.getCoupleId(),
                    event.getUserId(),
                    Activity.ActivityType.LOVE_MILESTONE,
                    title,
                    description,
                    null,
                    null
            );
        } catch (Exception e) {
            logger.error("Failed to create LOVE_MILESTONE activity: {}", e.getMessage());
        }
    }
    
    /**
     * 监听生理期记录事件
     */
    @EventListener
    public void handlePeriodRecordEvent(PeriodRecordEvent event) {
        try {
            String title = "记录了生理期信息";
            String description = "开始新的生理周期";
            
            activityService.createActivity(
                    event.getCoupleId(),
                    event.getUserId(),
                    Activity.ActivityType.PERIOD_RECORD,
                    title,
                    description,
                    event.getPeriodRecordId(),
                    "PeriodRecord"
            );
        } catch (Exception e) {
            logger.error("Failed to create PERIOD_RECORD activity: {}", e.getMessage());
        }
    }
    
    /**
     * 监听相册创建事件
     */
    @EventListener
    public void handleAlbumCreatedEvent(AlbumCreatedEvent event) {
        try {
            String title = "创建了相册\"" + event.getAlbumName() + "\"";
            String description = "整理照片，记录美好";
            
            activityService.createActivity(
                    event.getCoupleId(),
                    event.getUserId(),
                    Activity.ActivityType.ALBUM_CREATED,
                    title,
                    description,
                    event.getAlbumId(),
                    "Album"
            );
        } catch (Exception e) {
            logger.error("Failed to create ALBUM_CREATED activity: {}", e.getMessage());
        }
    }
    
    /**
     * 监听用户加入情侣事件
     */
    @EventListener
    public void handleUserJoinedEvent(UserJoinedEvent event) {
        try {
            String title = "成功配对，开始恋爱日记";
            String description = "欢迎加入恋爱日记，一起记录美好时光！";
            
            activityService.createActivity(
                    event.getCoupleId(),
                    event.getUserId(),
                    Activity.ActivityType.USER_JOINED,
                    title,
                    description,
                    null,
                    null
            );
        } catch (Exception e) {
            logger.error("Failed to create USER_JOINED activity: {}", e.getMessage());
        }
    }
    
    /**
     * 监听恋爱日期设置事件
     */
    @EventListener
    public void handleLoveDateSetEvent(LoveDateSetEvent event) {
        try {
            String title = "设置了恋爱开始日期";
            String description = "从" + event.getLoveStartDate() + "开始，我们的爱情故事正式开启";
            
            activityService.createActivity(
                    event.getCoupleId(),
                    event.getUserId(),
                    Activity.ActivityType.LOVE_DATE_SET,
                    title,
                    description,
                    null,
                    null
            );
        } catch (Exception e) {
            logger.error("Failed to create LOVE_DATE_SET activity: {}", e.getMessage());
        }
    }
}