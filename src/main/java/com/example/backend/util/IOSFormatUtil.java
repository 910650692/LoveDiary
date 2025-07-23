package com.example.backend.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * iOS格式处理工具类
 * 用于处理iOS特有的文件格式，如LIVP、HEIC等
 */
public class IOSFormatUtil {
    
    /**
     * 检查是否为iOS实况图片（LIVP格式）
     * LIVP文件实际上是一个ZIP压缩包，包含JPEG图片和MOV视频
     */
    public static boolean isLivePhoto(String fileName) {
        if (fileName == null) return false;
        String lowerFileName = fileName.toLowerCase();
        return lowerFileName.endsWith(".livp") || lowerFileName.endsWith(".live");
    }
    
    /**
     * 检查是否为HEIC格式
     */
    public static boolean isHeicFormat(String fileName) {
        if (fileName == null) return false;
        String lowerFileName = fileName.toLowerCase();
        return lowerFileName.endsWith(".heic") || lowerFileName.endsWith(".heif");
    }
    
    /**
     * 检查是否为iOS视频格式
     */
    public static boolean isIOSVideoFormat(String fileName) {
        if (fileName == null) return false;
        String lowerFileName = fileName.toLowerCase();
        return lowerFileName.endsWith(".m4v") || lowerFileName.endsWith(".3gp");
    }
    
    /**
     * 获取文件类型描述
     */
    public static String getFileTypeDescription(String fileName) {
        if (fileName == null) return "未知格式";
        
        String lowerFileName = fileName.toLowerCase();
        
        if (lowerFileName.endsWith(".livp")) {
            return "iOS实况图片";
        } else if (lowerFileName.endsWith(".heic") || lowerFileName.endsWith(".heif")) {
            return "HEIC图片";
        } else if (lowerFileName.endsWith(".m4v")) {
            return "iOS视频";
        } else if (lowerFileName.endsWith(".3gp")) {
            return "移动视频";
        } else if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg")) {
            return "JPEG图片";
        } else if (lowerFileName.endsWith(".png")) {
            return "PNG图片";
        } else if (lowerFileName.endsWith(".gif")) {
            return "GIF图片";
        } else if (lowerFileName.endsWith(".mp4")) {
            return "MP4视频";
        } else if (lowerFileName.endsWith(".mov")) {
            return "MOV视频";
        }
        
        return "其他格式";
    }
    
    /**
     * 检查文件是否为图片格式（包括iOS格式）
     */
    public static boolean isImageFormat(String fileName) {
        if (fileName == null) return false;
        String lowerFileName = fileName.toLowerCase();
        
        return lowerFileName.matches(".*\\.(jpg|jpeg|png|gif|bmp|webp|heic|heif|livp|live)$");
    }
    
    /**
     * 检查文件是否为视频格式（包括iOS格式）
     */
    public static boolean isVideoFormat(String fileName) {
        if (fileName == null) return false;
        String lowerFileName = fileName.toLowerCase();
        
        return lowerFileName.matches(".*\\.(mp4|avi|mov|wmv|flv|webm|m4v|3gp)$");
    }
    
    /**
     * 获取文件扩展名
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
    }
    
    /**
     * 验证LIVP文件结构（检查是否为有效的ZIP压缩包）
     */
    public static boolean validateLIVPStructure(Path filePath) {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(filePath))) {
            ZipEntry entry;
            boolean hasImage = false;
            boolean hasVideo = false;
            
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName().toLowerCase();
                
                if (entryName.endsWith(".jpg") || entryName.endsWith(".jpeg")) {
                    hasImage = true;
                } else if (entryName.endsWith(".mov")) {
                    hasVideo = true;
                }
                
                zis.closeEntry();
            }
            
            // LIVP文件应该包含至少一张图片和一个视频
            return hasImage && hasVideo;
            
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * 获取LIVP文件中的图片数量
     */
    public static int getLIVPImageCount(Path filePath) {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(filePath))) {
            ZipEntry entry;
            int imageCount = 0;
            
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName().toLowerCase();
                
                if (entryName.endsWith(".jpg") || entryName.endsWith(".jpeg")) {
                    imageCount++;
                }
                
                zis.closeEntry();
            }
            
            return imageCount;
            
        } catch (IOException e) {
            return 0;
        }
    }
    
    /**
     * 获取LIVP文件中的视频数量
     */
    public static int getLIVPVideoCount(Path filePath) {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(filePath))) {
            ZipEntry entry;
            int videoCount = 0;
            
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName().toLowerCase();
                
                if (entryName.endsWith(".mov")) {
                    videoCount++;
                }
                
                zis.closeEntry();
            }
            
            return videoCount;
            
        } catch (IOException e) {
            return 0;
        }
    }
} 