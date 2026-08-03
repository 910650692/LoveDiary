package com.example.backend.respository;

import com.example.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

/**
 * 用户数据访问接口
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 根据用户名查找用户
     */
    Optional<User> findByUsername(String username);
    
    /**
     * 根据邮箱查找用户
     */
    Optional<User> findByEmail(String email);
    
    /**
     * 根据邀请码查找用户
     */
    Optional<User> findByInvitationCode(String invitationCode);
    
    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);
    
    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);
    
    /**
     * 检查邀请码是否存在
     */
    boolean existsByInvitationCode(String invitationCode);
    
    /**
     * 根据情侣ID查找用户
     */
    List<User> findByCoupleId(Long coupleId);
    
    /**
     * 查找单身用户（可以匹配的用户）
     */
    @Query("SELECT u FROM User u WHERE u.status = 'SINGLE' AND u.isDeleted = false")
    List<User> findSingleUsers();
    
    /**
     * 根据用户名或昵称模糊搜索用户
     */
    @Query("SELECT u FROM User u WHERE (u.username LIKE %:keyword% OR u.nickname LIKE %:keyword%) AND u.isDeleted = false")
    List<User> searchUsers(@Param("keyword") String keyword);
    
    /**
     * 查找用户名或邮箱匹配的用户（用于登录）
     */
    @Query("SELECT u FROM User u WHERE (u.username = :identifier OR u.email = :identifier) AND u.isDeleted = false")
    Optional<User> findByUsernameOrEmail(@Param("identifier") String identifier);
} 