package com.example.backend.event;

/**
 * TodoItem完成事件
 */
public class TodoCompletedEvent {
    private final Long coupleId;
    private final Long userId;
    private final Long todoId;
    private final String todoTitle;
    private final String todoDescription;
    
    public TodoCompletedEvent(Long coupleId, Long userId, Long todoId, String todoTitle, String todoDescription) {
        this.coupleId = coupleId;
        this.userId = userId;
        this.todoId = todoId;
        this.todoTitle = todoTitle;
        this.todoDescription = todoDescription;
    }
    
    // Getters
    public Long getCoupleId() { return coupleId; }
    public Long getUserId() { return userId; }
    public Long getTodoId() { return todoId; }
    public String getTodoTitle() { return todoTitle; }
    public String getTodoDescription() { return todoDescription; }
}