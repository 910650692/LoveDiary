package com.example.backend.service;

import com.example.backend.model.User;
import com.example.backend.model.Couple;
import com.example.backend.respository.UserRepository;
import com.example.backend.respository.CoupleRepository;
import com.example.backend.config.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 用户服务类
 * 处理用户注册、登录、匹配等核心业务逻辑
 */
@Service
@Transactional
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CoupleRepository coupleRepository;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    private static final String INVITATION_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int INVITATION_CODE_LENGTH = 6;
    private static final int MAX_RETRY_COUNT = 10;

    
    /**
     * 用户注册
     * 注册成功后自动生成唯一的邀请码
     */
    public Map<String, Object> register(Map<String, String> registerData) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String username = registerData.get("username");
            String password = registerData.get("password");
            String nickname = registerData.get("nickname");
            String email = registerData.get("email");
            
            // 验证必填字段
            if (username == null || username.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "用户名不能为空");
                return result;
            }
            
            if (password == null || password.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "密码不能为空");
                return result;
            }
            
            if (nickname == null || nickname.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "昵称不能为空");
                return result;
            }
            
            // 检查用户名是否已存在
            if (userRepository.existsByUsername(username)) {
                result.put("success", false);
                result.put("message", "用户名已存在");
                return result;
            }
            
            // 检查邮箱是否已存在（如果提供了邮箱）
            if (email != null && !email.trim().isEmpty() && userRepository.existsByEmail(email)) {
                result.put("success", false);
                result.put("message", "邮箱已被注册");
                return result;
            }
            
            // 创建新用户
            User user = new User();
            user.setUsername(username.trim());
            user.setPassword(hashPassword(password)); // 密码加密
            user.setNickname(nickname.trim());
            user.setEmail(email != null ? email.trim() : null);
            user.setStatus(User.UserStatus.SINGLE);
            user.setIsDeleted(false);
            
            // 设置默认头像（根据性别）
            String genderStr = registerData.get("gender");
            if (genderStr != null) {
                try {
                    User.Gender gender = User.Gender.valueOf(genderStr.toUpperCase());
                    user.setGender(gender);
                    // 设置默认头像
                    if (gender == User.Gender.FEMALE) {
                        user.setAvatarUrl("/images/default-female.png");
                    } else {
                        user.setAvatarUrl("/images/default-male.png");
                    }
                } catch (IllegalArgumentException e) {
                    // 性别值无效，使用默认男性头像
                    user.setGender(User.Gender.MALE);
                    user.setAvatarUrl("/images/default-male.png");
                }
            } else {
                // 没有指定性别，使用默认男性头像
                user.setGender(User.Gender.MALE);
                user.setAvatarUrl("/images/default-male.png");
            }
            
            // 生成唯一邀请码
            String invitationCode = generateUniqueInvitationCode();
            user.setInvitationCode(invitationCode);
            
            // 保存用户
            User savedUser = userRepository.save(user);
            
            result.put("success", true);
            result.put("message", "注册成功");
            result.put("user", createUserInfo(savedUser));
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "注册失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 用户登录，登录成功后生成JWT token
     */
    public Map<String, Object> login(String identifier, String password) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 根据用户名或邮箱查找用户
            Optional<User> userOpt = userRepository.findByUsernameOrEmail(identifier);
            
            if (!userOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "用户不存在");
                return result;
            }
            
            User user = userOpt.get();
            
            // 验证密码
            if (!verifyPassword(password, user.getPassword())) {
                result.put("success", false);
                result.put("message", "密码错误");
                return result;
            }
            
            // 生成JWT token
            String token = jwtUtil.generateToken(user.getId(), user.getUsername());
            
            result.put("success", true);
            result.put("message", "登录成功");
            result.put("token", token);
            result.put("user", createUserInfo(user));
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "登录失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 通过邀请码匹配情侣
     * 当前用户输入另一个用户的邀请码进行匹配
     */
    public Map<String, Object> matchWithInvitationCode(Long currentUserId, String invitationCode) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 获取当前用户
            Optional<User> currentUserOpt = userRepository.findById(currentUserId);
            if (!currentUserOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "当前用户不存在");
                return result;
            }
            
            User currentUser = currentUserOpt.get();
            
            // 检查当前用户是否可以匹配
            if (!currentUser.canMatch()) {
                result.put("success", false);
                result.put("message", "您的状态不允许匹配（可能已经有情侣了）");
                return result;
            }
            
            // 根据邀请码查找目标用户
            Optional<User> targetUserOpt = userRepository.findByInvitationCode(invitationCode);
            if (!targetUserOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "邀请码不存在");
                return result;
            }
            
            User targetUser = targetUserOpt.get();
            
            // 检查目标用户是否可以匹配
            if (!targetUser.canMatch()) {
                result.put("success", false);
                result.put("message", "对方用户状态不允许匹配");
                return result;
            }
            
            // 检查是否是同一个用户
            if (currentUser.getId().equals(targetUser.getId())) {
                result.put("success", false);
                result.put("message", "不能和自己匹配");
                return result;
            }
            
            // 检查是否已经是情侣
            if (coupleRepository.existsByUserIds(currentUser.getId(), targetUser.getId())) {
                result.put("success", false);
                result.put("message", "你们已经是情侣了");
                return result;
            }
            
            // 创建情侣关系
            Couple couple = new Couple();
            couple.setUser1Id(currentUser.getId());
            couple.setUser2Id(targetUser.getId());
            couple.setStatus(Couple.CoupleStatus.ACTIVE);
            couple.setIsDeleted(false);
            
            // 设置恋爱开始日期为当天（默认值）
            couple.setLoveStartDate(LocalDate.now());
            couple.setMatchDate(LocalDateTime.now());
            
            Couple savedCouple = coupleRepository.save(couple);
            
            // 更新用户状态
            currentUser.setStatus(User.UserStatus.MATCHED);
            currentUser.setCoupleId(savedCouple.getId());
            targetUser.setStatus(User.UserStatus.MATCHED);
            targetUser.setCoupleId(savedCouple.getId());
            
            userRepository.save(currentUser);
            userRepository.save(targetUser);
            
            result.put("success", true);
            result.put("message", "匹配成功！");
            result.put("couple", createCoupleInfo(savedCouple));
            result.put("partner", createUserInfo(targetUser));
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "匹配失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取用户信息
     */
    public Map<String, Object> getUserInfo(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            return createUserInfo(userOpt.get());
        }
        return null;
    }
    
    /**
     * 获取用户的情侣信息
     */
    public Map<String, Object> getCoupleInfo(Long userId) {
        Optional<Couple> coupleOpt = coupleRepository.findByUserId(userId);
        if (coupleOpt.isPresent()) {
            Couple couple = coupleOpt.get();
            
            // 获取另一半的用户信息
            Long partnerId = couple.getPartnerUserId(userId);
            if (partnerId != null) {
                Optional<User> partnerOpt = userRepository.findById(partnerId);
                if (partnerOpt.isPresent()) {
                    Map<String, Object> result = createCoupleInfo(couple);
                    result.put("partner", createUserInfo(partnerOpt.get()));
                    return result;
                }
            }
        }
        return null;
    }
    
    /**
     * 解除情侣关系
     */
    public Map<String, Object> breakUp(Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<Couple> coupleOpt = coupleRepository.findByUserId(userId);
            if (!coupleOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "您目前没有情侣关系");
                return result;
            }
            
            Couple couple = coupleOpt.get();
            
            // 获取两个用户
            Optional<User> user1Opt = userRepository.findById(couple.getUser1Id());
            Optional<User> user2Opt = userRepository.findById(couple.getUser2Id());
            
            if (user1Opt.isPresent() && user2Opt.isPresent()) {
                User user1 = user1Opt.get();
                User user2 = user2Opt.get();
                
                // 更新用户状态
                user1.setStatus(User.UserStatus.SINGLE);
                user1.setCoupleId(null);
                user2.setStatus(User.UserStatus.SINGLE);
                user2.setCoupleId(null);
                
                userRepository.save(user1);
                userRepository.save(user2);
            }
            
            // 软删除情侣关系
            couple.setIsDeleted(true);
            couple.setStatus(Couple.CoupleStatus.INACTIVE);
            coupleRepository.save(couple);
            
            result.put("success", true);
            result.put("message", "已解除情侣关系");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "解除关系失败：" + e.getMessage());
        }
        
        return result;
    }
    
    // ========== 私有辅助方法 ==========
    
    /**
     * 生成唯一的邀请码
     */
    private String generateUniqueInvitationCode() {
        SecureRandom random = new SecureRandom();
        String code;
        int retryCount = 0;
        
        do {
            StringBuilder sb = new StringBuilder(INVITATION_CODE_LENGTH);
            for (int i = 0; i < INVITATION_CODE_LENGTH; i++) {
                int index = random.nextInt(INVITATION_CODE_CHARS.length());
                sb.append(INVITATION_CODE_CHARS.charAt(index));
            }
            code = sb.toString();
            retryCount++;
            
            if (retryCount > MAX_RETRY_COUNT) {
                throw new RuntimeException("生成邀请码失败，请重试");
            }
            
        } while (userRepository.existsByInvitationCode(code));
        
        return code;
    }
    
    /**
     * 密码加密（简单实现，生产环境建议使用BCrypt）
     */
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(password.getBytes("UTF-8"));
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
            
        } catch (Exception e) {
            throw new RuntimeException("密码加密失败", e);
        }
    }
    
    /**
     * 验证密码
     */
    private boolean verifyPassword(String password, String hashedPassword) {
        return hashPassword(password).equals(hashedPassword);
    }
    
    /**
     * 创建用户信息Map（不包含敏感信息）
     */
    private Map<String, Object> createUserInfo(User user) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("email", user.getEmail());
        userInfo.put("invitationCode", user.getInvitationCode());
        userInfo.put("status", user.getStatus().name());
        userInfo.put("coupleId", user.getCoupleId());
        userInfo.put("gender", user.getGender() != null ? user.getGender().name() : null);
        userInfo.put("birthDate", user.getBirthDate());
        userInfo.put("avatarUrl", user.getAvatarUrl());

        userInfo.put("createdAt", user.getCreatedAt());
        return userInfo;
    }
    
    /**
     * 创建情侣信息Map
     */
    private Map<String, Object> createCoupleInfo(Couple couple) {
        Map<String, Object> coupleInfo = new HashMap<>();
        coupleInfo.put("id", couple.getId());
        coupleInfo.put("user1Id", couple.getUser1Id());
        coupleInfo.put("user2Id", couple.getUser2Id());
        coupleInfo.put("loveStartDate", couple.getLoveStartDate());
        coupleInfo.put("matchDate", couple.getMatchDate());
        coupleInfo.put("status", couple.getStatus().name());
        coupleInfo.put("createdAt", couple.getCreatedAt());
        return coupleInfo;
    }
    
    // ========== 新增方法 ==========
    
    /**
     * 更新用户资料
     */
    public Map<String, Object> updateProfile(Long userId, Map<String, Object> profileData) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (!userOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "用户不存在");
                return result;
            }
            
            User user = userOpt.get();
            boolean hasChanges = false;
            
            // 更新昵称
            if (profileData.containsKey("nickname")) {
                String nickname = (String) profileData.get("nickname");
                if (nickname != null && !nickname.trim().isEmpty()) {
                    user.setNickname(nickname.trim());
                    hasChanges = true;
                }
            }
            
            // 更新邮箱
            if (profileData.containsKey("email")) {
                String email = (String) profileData.get("email");
                if (email != null && !email.trim().isEmpty()) {
                    // 检查邮箱是否已被其他用户使用
                    if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
                        result.put("success", false);
                        result.put("message", "邮箱已被其他用户使用");
                        return result;
                    }
                    user.setEmail(email.trim());
                    hasChanges = true;
                }
            }
            
            // 更新手机号
            if (profileData.containsKey("phone")) {
                String phone = (String) profileData.get("phone");
                user.setPhone(phone);
                hasChanges = true;
            }
            
            // 更新性别
            if (profileData.containsKey("gender")) {
                String genderStr = (String) profileData.get("gender");
                if (genderStr != null) {
                    try {
                        User.Gender gender = User.Gender.valueOf(genderStr.toUpperCase());
                        user.setGender(gender);
                        hasChanges = true;
                    } catch (IllegalArgumentException e) {
                        result.put("success", false);
                        result.put("message", "性别值无效");
                        return result;
                    }
                }
            }
            
            // 更新生日
            if (profileData.containsKey("birthDate")) {
                String birthDateStr = (String) profileData.get("birthDate");
                if (birthDateStr != null && !birthDateStr.trim().isEmpty()) {
                    try {
                        LocalDate birthDate = LocalDate.parse(birthDateStr);
                        user.setBirthDate(birthDate);
                        hasChanges = true;
                    } catch (Exception e) {
                        result.put("success", false);
                        result.put("message", "生日格式无效，请使用YYYY-MM-DD格式");
                        return result;
                    }
                }
            }
            
            if (hasChanges) {
                userRepository.save(user);
                result.put("success", true);
                result.put("message", "资料更新成功");
                result.put("user", createUserInfo(user));
            } else {
                result.put("success", false);
                result.put("message", "没有需要更新的内容");
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "更新失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 修改密码
     */
    public Map<String, Object> changePassword(Long userId, String oldPassword, String newPassword) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (!userOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "用户不存在");
                return result;
            }
            
            User user = userOpt.get();
            
            // 验证旧密码
            if (!verifyPassword(oldPassword, user.getPassword())) {
                result.put("success", false);
                result.put("message", "原密码错误");
                return result;
            }
            
            // 验证新密码
            if (newPassword == null || newPassword.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "新密码不能为空");
                return result;
            }
            
            if (newPassword.length() < 6) {
                result.put("success", false);
                result.put("message", "新密码长度不能少于6位");
                return result;
            }
            
            // 更新密码
            user.setPassword(hashPassword(newPassword));
            userRepository.save(user);
            
            result.put("success", true);
            result.put("message", "密码修改成功");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "密码修改失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取用户统计信息
     */
    public Map<String, Object> getUserStats(Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (!userOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "用户不存在");
                return result;
            }
            
            User user = userOpt.get();
            
            // 基础统计
            result.put("userId", userId);
            result.put("username", user.getUsername());
            result.put("nickname", user.getNickname());
            result.put("registrationDate", user.getCreatedAt());
            result.put("isMatched", user.isMatched());
            result.put("coupleId", user.getCoupleId());
            
            // 如果有情侣，获取情侣统计
            if (user.isMatched()) {
                Optional<Couple> coupleOpt = coupleRepository.findById(user.getCoupleId());
                if (coupleOpt.isPresent()) {
                    Couple couple = coupleOpt.get();
                    result.put("matchDate", couple.getMatchDate());
                    result.put("loveStartDate", couple.getLoveStartDate());
                    
                    // 计算恋爱天数
                    if (couple.getLoveStartDate() != null) {
                        long loveDays = java.time.temporal.ChronoUnit.DAYS.between(
                            couple.getLoveStartDate(), LocalDate.now());
                        result.put("loveDays", loveDays);
                    }
                }
            }
            
            result.put("success", true);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取统计信息失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 搜索用户（通过用户名或昵称）
     */
    public Map<String, Object> searchUsers(String keyword) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "搜索关键词不能为空");
                return result;
            }
            
            List<User> users = userRepository.searchUsers(keyword.trim());
            
            List<Map<String, Object>> userList = new ArrayList<>();
            for (User user : users) {
                if (!user.getIsDeleted()) {
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("id", user.getId());
                    userInfo.put("username", user.getUsername());
                    userInfo.put("nickname", user.getNickname());
                    userInfo.put("avatarUrl", user.getAvatarUrl());
                    userInfo.put("status", user.getStatus().name());
                    userList.add(userInfo);
                }
            }
            
            result.put("success", true);
            result.put("users", userList);
            result.put("count", userList.size());
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "搜索失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 检查用户名是否可用
     */
    public boolean isUsernameAvailable(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        return !userRepository.existsByUsername(username.trim());
    }
    
    /**
     * 检查邮箱是否可用
     */
    public boolean isEmailAvailable(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return !userRepository.existsByEmail(email.trim());
    }
} 