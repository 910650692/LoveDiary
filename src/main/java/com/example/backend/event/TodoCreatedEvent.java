package com.example.backend.event;

/**
 * TodoItem创建事件
 */
public class TodoCreatedEvent {
    private final Long coupleId;
    private final Long userId;
    private final Long todoId;
    private final String todoTitle;
    
    public TodoCreatedEvent(Long coupleId, Long userId, Long todoId, String todoTitle) {
        this.coupleId = coupleId;
        this.userId = userId;
        this.todoId = todoId;
        this.todoTitle = todoTitle;
    }
    
    // Getters
    public Long getCoupleId() { return coupleId; }
    public Long getUserId() { return userId; }
    public Long getTodoId() { return todoId; }
    public String getTodoTitle() { return todoTitle; }
}