package com.example.backend.respository;

import com.example.backend.model.PeriodRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 生理期记录数据访问接口
 */
@Repository
public interface PeriodRecordRepository extends JpaRepository<PeriodRecord, Long> {
    
    /**
     * 根据用户ID查找所有记录（按开始日期倒序）
     */
    List<PeriodRecord> findByUserIdAndIsDeletedFalseOrderByStartDateDesc(Long userId);
    
    /**
     * 根据情侣ID查找所有记录（按开始日期倒序）
     */
    List<PeriodRecord> findByCoupleIdAndIsDeletedFalseOrderByStartDateDesc(Long coupleId);
    
    /**
     * 根据用户ID和日期范围查找记录
     */
    List<PeriodRecord> findByUserIdAndStartDateBetweenAndIsDeletedFalseOrderByStartDateDesc(
            Long userId, LocalDate startDate, LocalDate endDate);
    
    /**
     * 根据用户ID查找实际记录（非预测记录）
     */
    List<PeriodRecord> findByUserIdAndIsPredictedFalseAndIsDeletedFalseOrderByStartDateDesc(Long userId);
    
    /**
     * 根据用户ID查找预测记录
     */
    List<PeriodRecord> findByUserIdAndIsPredictedTrueAndIsDeletedFalseOrderByStartDateDesc(Long userId);
    
    /**
     * 查找用户最近的N条实际记录（用于预测算法）
     */
    @Query(value = "SELECT * FROM period_records WHERE user_id = :userId AND is_predicted = false AND is_deleted = false " +
           "AND end_date IS NOT NULL ORDER BY start_date DESC LIMIT :limit", nativeQuery = true)
    List<PeriodRecord> findRecentCompletedRecords(@Param("userId") Long userId, @Param("limit") int limit);
    
    /**
     * 查找用户最新的一条记录
     */
    Optional<PeriodRecord> findFirstByUserIdAndIsDeletedFalseOrderByStartDateDesc(Long userId);
    
    /**
     * 查找用户最新的一条实际记录
     */
    Optional<PeriodRecord> findFirstByUserIdAndIsPredictedFalseAndIsDeletedFalseOrderByStartDateDesc(Long userId);
    
    /**
     * 查找指定日期范围内的记录（用于检查重复）
     */
    List<PeriodRecord> findByUserIdAndStartDateBetweenAndIsDeletedFalse(
            Long userId, LocalDate startDate, LocalDate endDate);
    
    /**
     * 查找用户在指定日期之后的预测记录（用于清理过期预测）
     */
    List<PeriodRecord> findByUserIdAndIsPredictedTrueAndStartDateAfterAndIsDeletedFalse(
            Long userId, LocalDate date);
    
    /**
     * 统计用户的记录总数
     */
    long countByUserIdAndIsDeletedFalse(Long userId);
    
    /**
     * 统计用户的实际记录数
     */
    long countByUserIdAndIsPredictedFalseAndIsDeletedFalse(Long userId);
    
    /**
     * 检查用户是否存在指定ID的记录（用于权限验证）
     */
    boolean existsByIdAndUserIdAndIsDeletedFalse(Long id, Long userId);
    
    /**
     * 根据ID和用户ID查找记录（用于权限验证）
     */
    Optional<PeriodRecord> findByIdAndUserIdAndIsDeletedFalse(Long id, Long userId);
}