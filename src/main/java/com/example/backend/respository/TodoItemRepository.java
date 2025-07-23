package com.example.backend.respository;

import com.example.backend.model.TodoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 待办事项数据访问接口（MVP版本）
 */
@Repository
public interface TodoItemRepository extends JpaRepository<TodoItem, Long> {
    
    /**
     * 根据情侣ID查找所有待办事项
     */
    List<TodoItem> findByCoupleIdAndIsDeletedFalseOrderByCreatedAtDesc(Long coupleId);
    
    /**
     * 根据情侣ID和状态查找待办事项
     */
    List<TodoItem> findByCoupleIdAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(Long coupleId, TodoItem.Status status);
    
    /**
     * 根据情侣ID和创建者ID查找待办事项
     */
    List<TodoItem> findByCoupleIdAndCreatorIdAndIsDeletedFalseOrderByCreatedAtDesc(Long coupleId, Long creatorId);
    
    /**
     * 根据情侣ID和完成者ID查找待办事项
     */
    List<TodoItem> findByCoupleIdAndCompleterIdAndIsDeletedFalseOrderByCreatedAtDesc(Long coupleId, Long completerId);
    
    /**
     * 根据标题或描述搜索待办事项
     */
    @Query("SELECT t FROM TodoItem t WHERE t.coupleId = :coupleId AND (t.title LIKE %:keyword% OR t.description LIKE %:keyword%) AND t.isDeleted = false ORDER BY t.createdAt DESC")
    List<TodoItem> searchByKeyword(@Param("coupleId") Long coupleId, @Param("keyword") String keyword);
    
    /**
     * 统计情侣的待办事项数量
     */
    long countByCoupleIdAndIsDeletedFalse(Long coupleId);
    
    /**
     * 统计情侣已完成的待办事项数量
     */
    long countByCoupleIdAndStatusAndIsDeletedFalse(Long coupleId, TodoItem.Status status);
    
    /**
     * 根据ID查找待办事项（包含软删除的）
     */
    Optional<TodoItem> findByIdAndIsDeletedFalse(Long id);
    
    /**
     * 检查待办事项是否属于指定情侣
     */
    boolean existsByIdAndCoupleIdAndIsDeletedFalse(Long id, Long coupleId);
} 