package com.example.backend.controller;

import com.example.backend.model.Anniversary;
import com.example.backend.service.AnniversaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

/**
 * 纪念日控制器 - 简单版本
 * 这里定义API接口，处理HTTP请求
 */
@RestController  // 标记这是REST API控制器
@RequestMapping("/api/anniversaries")  // 所有方法的URL都以这个开头
public class AnniversaryController {
    
    @Autowired  // 注入Service
    private AnniversaryService anniversaryService;
    
    /**
     * 获取所有纪念日
     * GET /api/anniversaries
     */
    @GetMapping
    public List<Anniversary> getAllAnniversaries() {
        return anniversaryService.getAllAnniversaries();
    }
    
    /**
     * 根据情侣ID获取纪念日
     * GET /api/anniversaries/couple/1
     */
    @GetMapping("/couple/{coupleId}")
    public List<Anniversary> getAnniversariesByCoupleId(@PathVariable Long coupleId) {
        return anniversaryService.getAnniversariesByCoupleId(coupleId);
    }
    
    /**
     * 根据ID获取单个纪念日
     * GET /api/anniversaries/1
     */
    @GetMapping("/{id}")
    public Optional<Anniversary> getAnniversaryById(@PathVariable Long id) {
        return anniversaryService.getAnniversaryById(id);
    }
    
    /**
     * 创建新纪念日
     * POST /api/anniversaries
     */
    @PostMapping
    public Anniversary createAnniversary(@RequestBody Anniversary anniversary) {
        return anniversaryService.saveAnniversary(anniversary);
    }
    
    /**
     * 更新纪念日
     * PUT /api/anniversaries/1
     */
    @PutMapping("/{id}")
    public Anniversary updateAnniversary(@PathVariable Long id, @RequestBody Anniversary anniversary) {
        anniversary.setId(id);  // 确保使用URL中的ID
        return anniversaryService.saveAnniversary(anniversary);
    }
    
    /**
     * 删除纪念日
     * DELETE /api/anniversaries/1
     */
    @DeleteMapping("/{id}")
    public String deleteAnniversary(@PathVariable Long id) {
        anniversaryService.deleteAnniversary(id);
        return "删除成功";
    }
    
    /**
     * 搜索纪念日
     * GET /api/anniversaries/search?keyword=生日
     */
    @GetMapping("/search")
    public List<Anniversary> searchAnniversaries(@RequestParam String keyword) {
        return anniversaryService.searchAnniversaries(keyword);
    }
}
