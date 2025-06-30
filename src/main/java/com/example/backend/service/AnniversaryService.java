package com.example.backend.service;

import com.example.backend.model.Anniversary;
import com.example.backend.respository.AnniversaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

/**
 * 纪念日业务逻辑类 - 简单版本
 * 这里写我们的业务逻辑，调用Repository操作数据库
 */
@Service  // 告诉Spring这是一个服务类
public class AnniversaryService {
    
    @Autowired  // 自动注入Repository
    private AnniversaryRepository anniversaryRepository;
    
    /**
     * 获取所有纪念日
     */
    public List<Anniversary> getAllAnniversaries() {
        return anniversaryRepository.findAll();
    }
    
    /**
     * 根据情侣ID获取纪念日
     */
    public List<Anniversary> getAnniversariesByCoupleId(Long coupleId) {
        return anniversaryRepository.findByCoupleId(coupleId);
    }
    
    /**
     * 根据ID获取单个纪念日
     */
    public Optional<Anniversary> getAnniversaryById(Long id) {
        return anniversaryRepository.findById(id);
    }
    
    /**
     * 保存纪念日（新增或更新）
     */
    public Anniversary saveAnniversary(Anniversary anniversary) {
        return anniversaryRepository.save(anniversary);
    }
    
    /**
     * 删除纪念日
     */
    public void deleteAnniversary(Long id) {
        anniversaryRepository.deleteById(id);
    }
    
    /**
     * 搜索纪念日
     */
    public List<Anniversary> searchAnniversaries(String keyword) {
        return anniversaryRepository.findByNameContaining(keyword);
    }
} 