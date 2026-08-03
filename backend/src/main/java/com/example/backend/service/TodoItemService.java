package com.example.backend.service;

import com.example.backend.model.TodoItem;
import com.example.backend.model.Couple;
import com.example.backend.respository.TodoItemRepository;
import com.example.backend.respository.CoupleRepository;
import com.example.backend.event.TodoCompletedEvent;
import com.example.backend.event.TodoCreatedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 待办事项服务类（MVP版本）
 * 处理待办事项的基础业务逻辑
 */
@Service
@Transactional
public class TodoItemService {
    
    @Autowired
    private TodoItemRepository todoItemRepository;
    
    @Autowired
    private CoupleRepository coupleRepository;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    /**
     * 创建待办事项
     */
    public Map<String, Object> createTodoItem(Long coupleId, Long creatorId, Map<String, Object> todoData) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 验证情侣关系
            Optional<Couple> coupleOpt = coupleRepository.findById(coupleId);
            if (!coupleOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "情侣关系不存在");
                return result;
            }
            
            Couple couple = coupleOpt.get();
            if (!couple.containsUser(creatorId)) {
                result.put("success", false);
                result.put("message", "您不属于这个情侣关系");
                return result;
            }
            
            // 创建待办事项
            TodoItem todoItem = new TodoItem();
            todoItem.setCoupleId(coupleId);
            todoItem.setCreatorId(creatorId);
            todoItem.setTitle((String) todoData.get("title"));
            todoItem.setDescription((String) todoData.get("description"));
            
            TodoItem savedTodo = todoItemRepository.save(todoItem);
            
            // 发布TodoCreated事件
            eventPublisher.publishEvent(new TodoCreatedEvent(
                coupleId, 
                creatorId, 
                savedTodo.getId(), 
                savedTodo.getTitle()
            ));
            
            result.put("success", true);
            result.put("message", "待办事项创建成功");
            result.put("data", createTodoItemInfo(savedTodo));
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "创建失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取情侣的所有待办事项
     */
    public Map<String, Object> getTodoItems(Long coupleId, Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 验证权限
            if (!validateCoupleAccess(coupleId, userId)) {
                result.put("success", false);
                result.put("message", "无权访问此情侣的待办事项");
                return result;
            }
            
            List<TodoItem> todoItems = todoItemRepository.findByCoupleIdAndIsDeletedFalseOrderByCreatedAtDesc(coupleId);
            List<Map<String, Object>> todoList = todoItems.stream()
                    .map(this::createTodoItemInfo)
                    .collect(Collectors.toList());
            
            result.put("success", true);
            result.put("data", todoList);
            result.put("count", todoList.size());
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 根据状态获取待办事项
     */
    public Map<String, Object> getTodoItemsByStatus(Long coupleId, Long userId, String status) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!validateCoupleAccess(coupleId, userId)) {
                result.put("success", false);
                result.put("message", "无权访问此情侣的待办事项");
                return result;
            }
            
            TodoItem.Status todoStatus;
            try {
                todoStatus = TodoItem.Status.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                result.put("success", false);
                result.put("message", "状态值无效");
                return result;
            }
            
            List<TodoItem> todoItems = todoItemRepository.findByCoupleIdAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(coupleId, todoStatus);
            List<Map<String, Object>> todoList = todoItems.stream()
                    .map(this::createTodoItemInfo)
                    .collect(Collectors.toList());
            
            result.put("success", true);
            result.put("data", todoList);
            result.put("count", todoList.size());
            result.put("status", status);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 更新待办事项
     */
    public Map<String, Object> updateTodoItem(Long todoId, Long userId, Map<String, Object> updateData) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<TodoItem> todoOpt = todoItemRepository.findByIdAndIsDeletedFalse(todoId);
            if (!todoOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "待办事项不存在");
                return result;
            }
            
            TodoItem todoItem = todoOpt.get();
            
            // 验证权限
            if (!validateCoupleAccess(todoItem.getCoupleId(), userId)) {
                result.put("success", false);
                result.put("message", "无权修改此待办事项");
                return result;
            }
            
            // 更新字段
            if (updateData.containsKey("title")) {
                todoItem.setTitle((String) updateData.get("title"));
            }
            
            if (updateData.containsKey("description")) {
                todoItem.setDescription((String) updateData.get("description"));
            }
            
            TodoItem updatedTodo = todoItemRepository.save(todoItem);
            
            result.put("success", true);
            result.put("message", "待办事项更新成功");
            result.put("data", createTodoItemInfo(updatedTodo));
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "更新失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 完成待办事项
     */
    public Map<String, Object> completeTodoItem(Long todoId, Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<TodoItem> todoOpt = todoItemRepository.findByIdAndIsDeletedFalse(todoId);
            if (!todoOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "待办事项不存在");
                return result;
            }
            
            TodoItem todoItem = todoOpt.get();
            
            // 验证权限
            if (!validateCoupleAccess(todoItem.getCoupleId(), userId)) {
                result.put("success", false);
                result.put("message", "无权操作此待办事项");
                return result;
            }
            
            if (todoItem.isCompleted()) {
                result.put("success", false);
                result.put("message", "待办事项已完成");
                return result;
            }
            
            todoItem.setStatus(TodoItem.Status.COMPLETED);
            todoItem.setCompleterId(userId);
            todoItem.setCompletedAt(LocalDateTime.now());
            
            TodoItem completedTodo = todoItemRepository.save(todoItem);
            
            // 发布TodoCompleted事件
            eventPublisher.publishEvent(new TodoCompletedEvent(
                todoItem.getCoupleId(),
                userId,
                todoItem.getId(),
                todoItem.getTitle(),
                todoItem.getDescription()
            ));
            
            result.put("success", true);
            result.put("message", "待办事项已完成");
            result.put("data", createTodoItemInfo(completedTodo));
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "操作失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 删除待办事项（软删除）
     */
    public Map<String, Object> deleteTodoItem(Long todoId, Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<TodoItem> todoOpt = todoItemRepository.findByIdAndIsDeletedFalse(todoId);
            if (!todoOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "待办事项不存在");
                return result;
            }
            
            TodoItem todoItem = todoOpt.get();
            
            // 验证权限
            if (!validateCoupleAccess(todoItem.getCoupleId(), userId)) {
                result.put("success", false);
                result.put("message", "无权删除此待办事项");
                return result;
            }
            
            todoItem.setIsDeleted(true);
            todoItemRepository.save(todoItem);
            
            result.put("success", true);
            result.put("message", "待办事项已删除");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "删除失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 搜索待办事项
     */
    public Map<String, Object> searchTodoItems(Long coupleId, Long userId, String keyword) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!validateCoupleAccess(coupleId, userId)) {
                result.put("success", false);
                result.put("message", "无权访问此情侣的待办事项");
                return result;
            }
            
            List<TodoItem> todoItems = todoItemRepository.searchByKeyword(coupleId, keyword);
            List<Map<String, Object>> todoList = todoItems.stream()
                    .map(this::createTodoItemInfo)
                    .collect(Collectors.toList());
            
            result.put("success", true);
            result.put("data", todoList);
            result.put("count", todoList.size());
            result.put("keyword", keyword);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "搜索失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取待办事项统计信息
     */
    public Map<String, Object> getTodoStats(Long coupleId, Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!validateCoupleAccess(coupleId, userId)) {
                result.put("success", false);
                result.put("message", "无权访问此情侣的待办事项");
                return result;
            }
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("total", todoItemRepository.countByCoupleIdAndIsDeletedFalse(coupleId));
            stats.put("completed", todoItemRepository.countByCoupleIdAndStatusAndIsDeletedFalse(coupleId, TodoItem.Status.COMPLETED));
            stats.put("pending", todoItemRepository.countByCoupleIdAndStatusAndIsDeletedFalse(coupleId, TodoItem.Status.PENDING));
            
            // 计算完成率
            long total = (Long) stats.get("total");
            long completed = (Long) stats.get("completed");
            if (total > 0) {
                double completionRate = (double) completed / total * 100;
                stats.put("completionRate", Math.round(completionRate * 100.0) / 100.0);
            } else {
                stats.put("completionRate", 0.0);
            }
            
            result.put("success", true);
            result.put("stats", stats);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取统计信息失败：" + e.getMessage());
        }
        
        return result;
    }
    
    // ========== 私有辅助方法 ==========
    
    /**
     * 验证用户是否有权限访问情侣的待办事项
     */
    private boolean validateCoupleAccess(Long coupleId, Long userId) {
        Optional<Couple> coupleOpt = coupleRepository.findById(coupleId);
        if (!coupleOpt.isPresent()) {
            return false;
        }
        
        Couple couple = coupleOpt.get();
        return couple.containsUser(userId);
    }
    
    /**
     * 创建待办事项信息Map
     */
    private Map<String, Object> createTodoItemInfo(TodoItem todoItem) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", todoItem.getId());
        info.put("coupleId", todoItem.getCoupleId());
        info.put("creatorId", todoItem.getCreatorId());
        info.put("completerId", todoItem.getCompleterId());
        info.put("title", todoItem.getTitle());
        info.put("description", todoItem.getDescription());
        info.put("status", todoItem.getStatus().name());
        info.put("completedAt", todoItem.getCompletedAt());
        info.put("createdAt", todoItem.getCreatedAt());
        info.put("updatedAt", todoItem.getUpdatedAt());
        info.put("isCompleted", todoItem.isCompleted());
        
        return info;
    }
} 