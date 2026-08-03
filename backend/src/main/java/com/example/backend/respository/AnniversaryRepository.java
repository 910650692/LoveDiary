package com.example.backend.respository;

import com.example.backend.model.Anniversary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * 纪念日数据访问接口 - 简单版本
 * 这个接口继承JpaRepository，Spring会自动实现基本的数据库操作
 */
@Repository
public interface AnniversaryRepository extends JpaRepository<Anniversary, Long> {
    
    // Spring Data JPA会根据方法名自动生成SQL
    // 这个方法会生成：SELECT * FROM anniversaries WHERE couple_id = ?
    List<Anniversary> findByCoupleId(Long coupleId);
    
    // 这个方法会生成：SELECT * FROM anniversaries WHERE name LIKE %?%
    List<Anniversary> findByNameContaining(String name);
}
