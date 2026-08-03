package com.example.backend.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务接口
 * 用于抽象文件存储操作，支持本地存储和云存储
 */
public interface FileStorageService {
    
    /**
     * 上传文件
     * @param file 要上传的文件
     * @param path 存储路径
     * @return 文件访问URL
     */
    String upload(MultipartFile file, String path);
    
    /**
     * 删除文件
     * @param filePath 文件路径
     */
    void delete(String filePath);
    
    /**
     * 获取文件访问URL
     * @param filePath 文件路径
     * @return 文件访问URL
     */
    String getFileUrl(String filePath);
    
    /**
     * 检查文件是否存在
     * @param filePath 文件路径
     * @return 是否存在
     */
    boolean exists(String filePath);
    
    /**
     * 获取文件大小
     * @param filePath 文件路径
     * @return 文件大小（字节）
     */
    long getFileSize(String filePath);
    
    /**
     * 检查是否为iOS实况图片格式
     * @param fileName 文件名
     * @return 是否为实况图片
     */
    boolean isLivePhoto(String fileName);
    
    /**
     * 检查是否为HEIC格式
     * @param fileName 文件名
     * @return 是否为HEIC格式
     */
    boolean isHeicFormat(String fileName);
    
    /**
     * 获取文件的实际扩展名（处理iOS特殊格式）
     * @param fileName 文件名
     * @return 文件扩展名
     */
    String getActualFileExtension(String fileName);
} 