package com.example.backend.respository;

import com.example.backend.model.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户动态数据访问接口
 */
@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    
    /**
     * 根据情侣ID查找最近的动态（分页）
     */
    Page<Activity> findByCoupleIdAndIsDeletedFalseOrderByCreatedAtDesc(Long coupleId, Pageable pageable);
    
    /**
     * 根据情侣ID查找最近的动态（限制数量）
     */
    List<Activity> findTop20ByCoupleIdAndIsDeletedFalseOrderByCreatedAtDesc(Long coupleId);
    
    /**
     * 根据情侣ID和动态类型查找动态
     */
    List<Activity> findByCoupleIdAndActivityTypeAndIsDeletedFalseOrderByCreatedAtDesc(Long coupleId, Activity.ActivityType activityType);
    
    /**
     * 根据情侣ID和用户ID查找动态
     */
    List<Activity> findByCoupleIdAndUserIdAndIsDeletedFalseOrderByCreatedAtDesc(Long coupleId, Long userId);
    
    /**
     * 根据情侣ID和时间范围查找动态
     */
    @Query("SELECT a FROM Activity a WHERE a.coupleId = :coupleId AND a.createdAt BETWEEN :startDate AND :endDate AND a.isDeleted = false ORDER BY a.createdAt DESC")
    List<Activity> findByCoupleIdAndDateRange(@Param("coupleId") Long coupleId, 
                                            @Param("startDate") LocalDateTime startDate, 
                                            @Param("endDate") LocalDateTime endDate);
    
    /**
     * 根据情侣ID和引用信息查找动态
     */
    List<Activity> findByCoupleIdAndReferenceIdAndReferenceTypeAndIsDeletedFalse(Long coupleId, Long referenceId, String referenceType);
    
    /**
     * 查找最近7天的动态
     */
    @Query("SELECT a FROM Activity a WHERE a.coupleId = :coupleId AND a.createdAt >= :sevenDaysAgo AND a.isDeleted = false ORDER BY a.createdAt DESC")
    List<Activity> findRecentActivities(@Param("coupleId") Long coupleId, @Param("sevenDaysAgo") LocalDateTime sevenDaysAgo);
    
    /**
     * 查找今天的动态
     */
    @Query("SELECT a FROM Activity a WHERE a.coupleId = :coupleId AND DATE(a.createdAt) = CURRENT_DATE AND a.isDeleted = false ORDER BY a.createdAt DESC")
    List<Activity> findTodayActivities(@Param("coupleId") Long coupleId);
    
    /**
     * 统计情侣的动态总数
     */
    long countByCoupleIdAndIsDeletedFalse(Long coupleId);
    
    /**
     * 统计情侣某种类型的动态数量
     */
    long countByCoupleIdAndActivityTypeAndIsDeletedFalse(Long coupleId, Activity.ActivityType activityType);
    
    /**
     * 根据ID查找动态（排除软删除的）
     */
    Optional<Activity> findByIdAndIsDeletedFalse(Long id);
    
    /**
     * 检查动态是否属于指定情侣
     */
    boolean existsByIdAndCoupleIdAndIsDeletedFalse(Long id, Long coupleId);
    
    /**
     * 根据引用信息删除相关动态
     */
    @Modifying
    @Query("UPDATE Activity a SET a.isDeleted = true, a.deletedAt = :deletedAt WHERE a.referenceId = :referenceId AND a.referenceType = :referenceType AND a.isDeleted = false")
    void softDeleteByReference(@Param("referenceId") Long referenceId, 
                              @Param("referenceType") String referenceType, 
                              @Param("deletedAt") LocalDateTime deletedAt);
    
    /**
     * 根据情侣ID查找动态类型分布统计
     */
    @Query("SELECT a.activityType, COUNT(a) FROM Activity a WHERE a.coupleId = :coupleId AND a.isDeleted = false GROUP BY a.activityType ORDER BY COUNT(a) DESC")
    List<Object[]> getActivityTypeStatistics(@Param("coupleId") Long coupleId);
    
    /**
     * 获取情侣的活跃天数统计
     */
    @Query("SELECT COUNT(DISTINCT DATE(a.createdAt)) FROM Activity a WHERE a.coupleId = :coupleId AND a.isDeleted = false")
    long getActiveDaysCount(@Param("coupleId") Long coupleId);
    
    /**
     * 查找某个时间之前的动态（用于清理旧数据）
     */
    List<Activity> findByCoupleIdAndCreatedAtBeforeAndIsDeletedFalse(Long coupleId, LocalDateTime beforeDate);
}