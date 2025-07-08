package com.example.backend.respository;

import com.example.backend.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    
    // 获取情侣的所有未删除标签
    List<Tag> findByCoupleIdAndIsDeletedFalseOrderByCreatedAtDesc(Long coupleId);
    
    // 检查标签名是否已存在（同一情侣下）
    @Query("SELECT COUNT(t) > 0 FROM Tag t WHERE t.coupleId = :coupleId AND t.name = :name AND t.isDeleted = false")
    boolean existsByCoupleIdAndNameAndNotDeleted(@Param("coupleId") Long coupleId, @Param("name") String name);
    
    // 根据情侣ID和标签名查找标签（排除已删除的）
    Optional<Tag> findByCoupleIdAndNameAndIsDeletedFalse(Long coupleId, String name);
    
    // 获取标签（排除已删除的）
    Optional<Tag> findByIdAndIsDeletedFalse(Long id);
} 