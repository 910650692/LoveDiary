package com.example.backend.respository;

import com.example.backend.model.MoodRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MoodRepository extends JpaRepository<MoodRecord, Long> {

    /**
     * 根据用户ID和日期查找情绪记录
     */
    Optional<MoodRecord> findByUserIdAndRecordDateAndIsDeletedFalse(Long userId, LocalDate recordDate);

    /**
     * 根据用户ID和日期范围查找情绪记录
     */
    List<MoodRecord> findByUserIdAndRecordDateBetweenAndIsDeletedFalseOrderByRecordDateDesc(
            Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 根据情侣ID和日期范围查找情绪记录
     */
    List<MoodRecord> findByCoupleIdAndRecordDateBetweenAndIsDeletedFalseOrderByRecordDateDesc(
            Long coupleId, LocalDate startDate, LocalDate endDate);

    /**
     * 根据ID查找情绪记录
     */
    Optional<MoodRecord> findByIdAndIsDeletedFalse(Long id);

    /**
     * 检查情绪记录是否属于指定情侣
     */
    boolean existsByIdAndCoupleIdAndIsDeletedFalse(Long id, Long coupleId);

    /**
     * 统计用户在日期范围内的情绪记录数
     */
    long countByUserIdAndRecordDateBetweenAndIsDeletedFalse(
            Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 按情绪类型统计
     */
    @Query("SELECT m.moodType, COUNT(m) FROM MoodRecord m WHERE m.userId = :userId " +
           "AND m.recordDate BETWEEN :startDate AND :endDate AND m.isDeleted = false " +
           "GROUP BY m.moodType")
    List<Object[]> countByMoodType(@Param("userId") Long userId,
                                   @Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate);
}
