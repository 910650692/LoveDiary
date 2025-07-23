package com.example.backend.respository;

import com.example.backend.model.PhotoTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhotoTagRepository extends JpaRepository<PhotoTag, Long> {
    
    // 获取照片的所有标签ID
    @Query("SELECT pt.tagId FROM PhotoTag pt WHERE pt.photoId = :photoId")
    List<Long> findTagIdsByPhotoId(@Param("photoId") Long photoId);
    
    // 获取标签的所有照片ID
    @Query("SELECT pt.photoId FROM PhotoTag pt WHERE pt.tagId = :tagId")
    List<Long> findPhotoIdsByTagId(@Param("tagId") Long tagId);
    
    // 删除照片的所有标签关系
    void deleteByPhotoId(Long photoId);
    
    // 删除标签的所有关系
    void deleteByTagId(Long tagId);
} 