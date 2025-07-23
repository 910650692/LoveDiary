# iOS格式支持说明

## 概述

LoveDiary 照片管理系统现已支持iOS设备特有的文件格式，包括实况图片（Live Photos）和高效图片格式（HEIC/HEIF）。

## 支持的iOS格式

### 1. 实况图片（Live Photos）

#### 格式说明
- **文件扩展名**: `.livp`
- **文件类型**: ZIP压缩包
- **内容**: 包含一张JPEG图片和一个MOV视频文件
- **大小**: 通常比普通照片大2-3倍

#### 技术实现
- LIVP文件实际上是一个ZIP压缩包
- 包含以下文件：
  - `IMG_XXXX.JPG` - 主图片文件
  - `IMG_XXXX.MOV` - 对应的短视频文件
  - 可能包含元数据文件

#### 处理方式
- 系统将LIVP文件归类为照片类型
- 支持直接上传和存储
- 可以通过文件访问接口下载
- 在照片列表中显示为"iOS实况图片"

### 2. HEIC/HEIF格式

#### 格式说明
- **文件扩展名**: `.heic` / `.heif`
- **全称**: High Efficiency Image Container / High Efficiency Image Format
- **优势**: 比JPEG更小的文件大小，更好的图像质量
- **兼容性**: iOS 11+默认使用此格式

#### 技术实现
- 使用HEVC（H.265）编码
- 支持10位色深
- 支持透明度
- 文件大小比JPEG小约50%

#### 处理方式
- 系统将HEIC文件归类为照片类型
- 支持直接上传和存储
- 在照片列表中显示为"HEIC图片"

## 文件上传处理

### 支持的格式列表

#### 图片格式
- `jpg`, `jpeg` - JPEG图片
- `png` - PNG图片
- `gif` - GIF动画
- `bmp` - BMP图片
- `webp` - WebP图片
- `heic` - HEIC图片（iOS）
- `heif` - HEIF图片（iOS）
- `livp` - 实况图片（iOS）
- `live` - 实况图片（iOS）

#### 视频格式
- `mp4` - MP4视频
- `avi` - AVI视频
- `mov` - MOV视频（iOS）
- `wmv` - WMV视频
- `flv` - FLV视频
- `webm` - WebM视频
- `m4v` - M4V视频（iOS）
- `3gp` - 3GP视频（移动设备）

### 文件类型检测

系统通过以下方式检测文件类型：

1. **MIME类型检测**
   - 优先使用HTTP请求中的Content-Type
   - 支持`image/heic`、`image/heif`等iOS格式

2. **文件扩展名检测**
   - 检查文件名的扩展名
   - 支持`.livp`、`.heic`等iOS格式

3. **文件内容检测**
   - 对于LIVP文件，验证ZIP压缩包结构
   - 确保包含必要的图片和视频文件

## API接口

### 上传接口
```
POST /api/photos/upload
Content-Type: multipart/form-data

参数：
- coupleId: 情侣ID
- creatorId: 创建者ID
- albumId: 相册ID（可选）
- file: 文件（支持iOS格式）
- description: 描述（可选）
- location: 拍摄地点（可选）
- tags: 标签（可选）
```

### 响应示例
```json
{
  "success": true,
  "message": "照片上传成功",
  "photo": {
    "id": 1,
    "coupleId": 1,
    "albumId": 1,
    "creatorId": 1,
    "fileName": "photos/1/1/abc123.livp",
    "originalName": "IMG_1234.LIVP",
    "filePath": "photos/1/1/abc123.livp",
    "fileSize": 5242880,
    "fileType": "PHOTO",
    "mimeType": "application/octet-stream",
    "description": "iOS实况图片",
    "location": "北京",
    "tags": "实况图片,iOS",
    "isFavorite": false,
    "createdAt": "2024-01-01T10:00:00",
    "fileUrl": "/files/photos/1/1/abc123.livp",
    "fileTypeDescription": "iOS实况图片"
  }
}
```

## 文件访问

### 下载接口
```
GET /files/{filePath}
```

### 文件信息接口
```
GET /files/info/{filePath}
```

## 注意事项

### 1. 文件大小限制
- 默认最大文件大小：50MB
- 可在`application.properties`中配置
- LIVP文件通常较大，建议适当调整限制

### 2. 浏览器兼容性
- HEIC格式：需要现代浏览器支持
- LIVP格式：需要特殊播放器或解压工具
- 建议在移动端APP中处理这些格式

### 3. 存储空间
- iOS格式文件通常较大
- 建议定期清理不需要的文件
- 考虑使用云存储服务

### 4. 性能考虑
- LIVP文件解压需要额外处理时间
- HEIC文件可能需要格式转换
- 建议异步处理大文件

## 开发建议

### 1. 前端处理
- 使用专门的库处理HEIC格式（如heic2any）
- 对于LIVP文件，可以提取其中的JPEG图片显示
- 提供格式转换选项

### 2. 移动端APP
- 直接支持iOS原生格式
- 提供格式转换功能
- 优化上传体验

### 3. 服务端优化
- 考虑添加格式转换服务
- 实现文件压缩功能
- 添加文件完整性验证

## 测试

### 测试文件
- 准备各种iOS格式的测试文件
- 验证文件上传和下载功能
- 测试文件类型检测准确性

### 测试场景
1. 上传LIVP实况图片
2. 上传HEIC格式图片
3. 上传混合格式文件
4. 验证文件访问权限
5. 测试文件删除功能

## 未来扩展

### 1. 格式转换
- 自动将HEIC转换为JPEG
- 提取LIVP中的关键帧
- 生成缩略图

### 2. 元数据提取
- 提取EXIF信息
- 获取拍摄参数
- 地理位置信息

### 3. 智能分类
- 根据文件格式自动分类
- 识别实况图片
- 标记特殊格式

---

**注意**: iOS格式支持需要客户端配合，建议在移动端APP中实现完整的格式处理功能。 