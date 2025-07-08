package com.example.backend.respository;

import com.example.backend.model.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 照片数据访问接口（MVP版本）
 */
@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long> {
    
    /**
     * 根据情侣ID查找所有照片
     */
    List<Photo> findByCoupleIdAndIsDeletedFalseOrderByCreatedAtDesc(Long coupleId);
    
    /**
     * 根据相册ID查找照片
     */
    List<Photo> findByAlbumIdAndIsDeletedFalseOrderByCreatedAtDesc(Long albumId);
    
    /**
     * 根据情侣ID和相册ID查找照片
     */
    List<Photo> findByCoupleIdAndAlbumIdAndIsDeletedFalseOrderByCreatedAtDesc(Long coupleId, Long albumId);
    
    /**
     * 根据情侣ID查找收藏的照片
     */
    List<Photo> findByCoupleIdAndIsFavoriteTrueAndIsDeletedFalseOrderByCreatedAtDesc(Long coupleId);
    
    /**
     * 根据情侣ID和文件类型查找照片
     */
    List<Photo> findByCoupleIdAndFileTypeAndIsDeletedFalseOrderByCreatedAtDesc(Long coupleId, Photo.FileType fileType);
    
    /**
     * 根据情侣ID和创建者ID查找照片
     */
    List<Photo> findByCoupleIdAndCreatorIdAndIsDeletedFalseOrderByCreatedAtDesc(Long coupleId, Long creatorId);
    
    /**
     * 根据标题、描述或标签搜索照片
     */
    @Query("SELECT p FROM Photo p WHERE p.coupleId = :coupleId AND (p.description LIKE %:keyword% OR p.location LIKE %:keyword% OR p.tags LIKE %:keyword%) AND p.isDeleted = false ORDER BY p.createdAt DESC")
    List<Photo> searchByKeyword(@Param("coupleId") Long coupleId, @Param("keyword") String keyword);
    
    /**
     * 统计情侣的照片数量
     */
    long countByCoupleIdAndIsDeletedFalse(Long coupleId);
    
    /**
     * 统计相册的照片数量
     */
    long countByAlbumIdAndIsDeletedFalse(Long albumId);
    
    /**
     * 统计情侣收藏的照片数量
     */
    long countByCoupleIdAndIsFavoriteTrueAndIsDeletedFalse(Long coupleId);
    
    /**
     * 统计情侣的照片数量（按文件类型）
     */
    long countByCoupleIdAndFileTypeAndIsDeletedFalse(Long coupleId, Photo.FileType fileType);
    
    /**
     * 根据ID查找照片（包含软删除的）
     */
    Optional<Photo> findByIdAndIsDeletedFalse(Long id);
    
    /**
     * 检查照片是否属于指定情侣
     */
    boolean existsByIdAndCoupleIdAndIsDeletedFalse(Long id, Long coupleId);
    
    /**
     * 检查照片是否属于指定相册
     */
    boolean existsByIdAndAlbumIdAndIsDeletedFalse(Long id, Long albumId);
    
    /**
     * 获取相册的封面照片
     */
    Optional<Photo> findFirstByAlbumIdAndIsDeletedFalseOrderByCreatedAtDesc(Long albumId);
} 