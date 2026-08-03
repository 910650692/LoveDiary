package com.example.backend.respository;

import com.example.backend.model.Couple;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

/**
 * 情侣数据访问接口
 */
@Repository
public interface CoupleRepository extends JpaRepository<Couple, Long> {
    
    /**
     * 根据用户ID查找情侣（用户可能是user1或user2）
     */
    @Query("SELECT c FROM Couple c WHERE (c.user1Id = :userId OR c.user2Id = :userId) AND c.isDeleted = false")
    Optional<Couple> findByUserId(@Param("userId") Long userId);
    
    /**
     * 根据两个用户ID查找情侣关系
     */
    @Query("SELECT c FROM Couple c WHERE ((c.user1Id = :userId1 AND c.user2Id = :userId2) OR (c.user1Id = :userId2 AND c.user2Id = :userId1)) AND c.isDeleted = false")
    Optional<Couple> findByUserIds(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
    
    /**
     * 查找所有活跃的情侣
     */
    @Query("SELECT c FROM Couple c WHERE c.status = 'ACTIVE' AND c.isDeleted = false")
    List<Couple> findActiveCouples();
    

    
    /**
     * 检查两个用户是否已经是情侣
     */
    @Query("SELECT COUNT(c) > 0 FROM Couple c WHERE ((c.user1Id = :userId1 AND c.user2Id = :userId2) OR (c.user1Id = :userId2 AND c.user2Id = :userId1)) AND c.status = 'ACTIVE' AND c.isDeleted = false")
    boolean existsByUserIds(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
} 