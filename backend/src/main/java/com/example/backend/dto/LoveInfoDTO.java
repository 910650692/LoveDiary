package com.example.backend.dto;

import lombok.Data;
import java.time.LocalDate;

/**
 * 恋爱信息DTO
 * 用于获取和更新恋爱开始日期
 */
@Data
public class LoveInfoDTO {
    
    /**
     * 恋爱开始日期
     */
    private LocalDate loveStartDate;
    
    /**
     * 恋爱天数
     */
    private Long loveDays;
    
    /**
     * 恋爱月数
     */
    private Long loveMonths;
    
    /**
     * 恋爱年数
     */
    private Long loveYears;
} 