package com.example.backend.respository;

import com.example.backend.model.Album;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 相册数据访问接口（MVP版本）
 */
@Repository
public interface AlbumRepository extends JpaRepository<Album, Long> {
    
    /**
     * 根据情侣ID查找所有相册
     */
    List<Album> findByCoupleIdAndIsDeletedFalseOrderByCreatedAtDesc(Long coupleId);
    
    /**
     * 根据情侣ID和相册名称搜索相册
     */
    @Query("SELECT a FROM Album a WHERE a.coupleId = :coupleId AND (a.name LIKE %:keyword% OR a.description LIKE %:keyword%) AND a.isDeleted = false ORDER BY a.createdAt DESC")
    List<Album> searchByKeyword(@Param("coupleId") Long coupleId, @Param("keyword") String keyword);
    
    /**
     * 统计情侣的相册数量
     */
    long countByCoupleIdAndIsDeletedFalse(Long coupleId);
    
    /**
     * 根据ID查找相册（包含软删除的）
     */
    Optional<Album> findByIdAndIsDeletedFalse(Long id);
    
    /**
     * 检查相册是否属于指定情侣
     */
    boolean existsByIdAndCoupleIdAndIsDeletedFalse(Long id, Long coupleId);
    
    /**
     * 根据情侣ID查找有封面照片的相册
     */
    List<Album> findByCoupleIdAndCoverPhotoIdIsNotNullAndIsDeletedFalseOrderByCreatedAtDesc(Long coupleId);
} 