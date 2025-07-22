package com.example.backend.controller;

import com.example.backend.service.TodoItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 待办事项控制器（MVP版本）
 * 提供情侣共享待办事项的基础API接口
 */
@RestController
@RequestMapping("/api/todos")
public class TodoItemController {
    
    @Autowired
    private TodoItemService todoItemService;
    
    /**
     * 创建待办事项
     * POST /api/todos
     * {
     *   "title": "买生日礼物",
     *   "description": "为另一半准备生日礼物",
     *   "priority": "HIGH",
     *   "dueDate": "2024-01-15T18:00:00",
     *   "tags": ["礼物", "生日"],
     *   "isImportant": true
     * }
     */
    @PostMapping
    public Map<String, Object> createTodoItem(
            @RequestBody Map<String, Object> todoData,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Long coupleId = (Long) todoData.get("coupleId");
        return todoItemService.createTodoItem(coupleId, userId, todoData);
    }
    
    /**
     * 获取情侣的所有待办事项
     * GET /api/todos/couple/{coupleId}
     */
    @GetMapping("/couple/{coupleId}")
    public Map<String, Object> getTodoItems(
            @PathVariable Long coupleId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return todoItemService.getTodoItems(coupleId, userId);
    }
    
    /**
     * 根据状态获取待办事项
     * GET /api/todos/couple/{coupleId}/status/{status}
     */
    @GetMapping("/couple/{coupleId}/status/{status}")
    public Map<String, Object> getTodoItemsByStatus(
            @PathVariable Long coupleId,
            @PathVariable String status,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return todoItemService.getTodoItemsByStatus(coupleId, userId, status);
    }
    
    /**
     * 更新待办事项
     * PUT /api/todos/{todoId}
     * {
     *   "title": "更新后的标题",
     *   "description": "更新后的描述",
     *   "priority": "MEDIUM",
     *   "dueDate": "2024-01-20T18:00:00",
     *   "tags": ["更新", "任务"],
     *   "isImportant": false
     * }
     */
    @PutMapping("/{todoId}")
    public Map<String, Object> updateTodoItem(
            @PathVariable Long todoId,
            @RequestBody Map<String, Object> updateData,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return todoItemService.updateTodoItem(todoId, userId, updateData);
    }
    
    /**
     * 完成待办事项
     * PUT /api/todos/{todoId}/complete
     */
    @PutMapping("/{todoId}/complete")
    public Map<String, Object> completeTodoItem(
            @PathVariable Long todoId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return todoItemService.completeTodoItem(todoId, userId);
    }
    
    /**
     * 删除待办事项
     * DELETE /api/todos/{todoId}
     */
    @DeleteMapping("/{todoId}")
    public Map<String, Object> deleteTodoItem(
            @PathVariable Long todoId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return todoItemService.deleteTodoItem(todoId, userId);
    }
    
    /**
     * 搜索待办事项
     * GET /api/todos/couple/{coupleId}/search?keyword=关键词
     */
    @GetMapping("/couple/{coupleId}/search")
    public Map<String, Object> searchTodoItems(
            @PathVariable Long coupleId,
            @RequestParam String keyword,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return todoItemService.searchTodoItems(coupleId, userId, keyword);
    }
    
    /**
     * 获取待办事项统计信息
     * GET /api/todos/couple/{coupleId}/stats
     */
    @GetMapping("/couple/{coupleId}/stats")
    public Map<String, Object> getTodoStats(
            @PathVariable Long coupleId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return todoItemService.getTodoStats(coupleId, userId);
    }
} 