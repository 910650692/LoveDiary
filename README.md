# LoveDiary 情侣纪念日管理系统

## 项目概述

LoveDiary 是一个专为情侣设计的纪念日管理系统，帮助情侣记录和提醒重要的纪念日，包括恋爱开始日期、第一次约会、生日等特殊日子。

## 功能特性

### 用户管理
- ✅ 用户注册与登录
- ✅ JWT令牌认证
- ✅ 用户资料管理（昵称、邮箱、性别、生日等）
- ✅ 密码修改
- ✅ 头像上传与管理
- ✅ 邀请码生成与情侣匹配

### 情侣管理
- ✅ 通过邀请码匹配情侣
- ✅ 情侣关系状态管理
- ✅ 恋爱开始日期设置
- ✅ 关系时长计算
- ✅ 情侣里程碑记录

### 纪念日管理
- ✅ 纪念日增删改查
- ✅ 纪念日搜索功能
- ✅ 即将到来的纪念日提醒
- ✅ 纪念日概览统计
- ✅ 推送通知设置
- ✅ 批量推送管理

### 待办事项管理
- ✅ 情侣共享待办事项
- ✅ 状态管理（待完成、已完成）
- ✅ 基础CRUD操作
- ✅ 搜索功能
- ✅ 统计信息

### 系统功能
- ✅ 健康检查接口
- ✅ API文档自动生成
- ✅ 全局异常处理
- ✅ 统一错误响应格式

## 技术栈

- **后端框架**: Spring Boot 3.x
- **数据库**: H2 (开发环境)
- **ORM**: Spring Data JPA
- **认证**: JWT (JSON Web Token)
- **文件上传**: Spring Multipart
- **构建工具**: Maven

## 项目结构

```
src/main/java/com/example/backend/
├── config/                 # 配置类
│   ├── JwtUtil.java       # JWT工具类
│   └── JwtAuthenticationFilter.java # JWT认证过滤器
├── controller/            # 控制器层
│   ├── UserController.java        # 用户管理API
│   ├── AvatarController.java      # 头像管理API
│   ├── CoupleController.java      # 情侣管理API
│   ├── LoveInfoController.java    # 恋爱信息API
│   ├── AnniversaryController.java # 纪念日管理API
│   ├── TodoItemController.java    # 待办事项管理API
│   ├── HealthController.java      # 健康检查API
│   ├── ApiDocController.java      # API文档API
│   └── GlobalExceptionHandler.java # 全局异常处理
├── service/               # 服务层
│   ├── UserService.java           # 用户业务逻辑
│   ├── AvatarService.java         # 头像业务逻辑
│   ├── CoupleService.java         # 情侣业务逻辑
│   ├── LoveInfoService.java       # 恋爱信息业务逻辑
│   ├── AnniversaryService.java    # 纪念日业务逻辑
│   └── TodoItemService.java       # 待办事项业务逻辑
├── model/                 # 实体类
│   ├── User.java                 # 用户实体
│   ├── Couple.java               # 情侣实体
│   ├── Anniversary.java          # 纪念日实体
│   └── TodoItem.java             # 待办事项实体
├── repository/            # 数据访问层
│   ├── UserRepository.java       # 用户数据访问
│   ├── CoupleRepository.java     # 情侣数据访问
│   ├── AnniversaryRepository.java # 纪念日数据访问
│   └── TodoItemRepository.java   # 待办事项数据访问
└── dto/                   # 数据传输对象
    └── LoveInfoDTO.java          # 恋爱信息DTO
```

## API接口总览

### 用户管理 API (`/api/users`)
- `POST /register` - 用户注册
- `POST /login` - 用户登录
- `GET /me` - 获取当前用户信息
- `PUT /{userId}/profile` - 更新用户资料
- `PUT /{userId}/password` - 修改密码
- `POST /{userId}/match` - 通过邀请码匹配情侣
- `GET /me/couple` - 获取情侣信息
- `DELETE /{userId}/couple` - 解除情侣关系
- `GET /me/invitation-code` - 获取邀请码
- `GET /me/stats` - 获取用户统计信息
- `GET /search` - 搜索用户
- `GET /check-username` - 检查用户名可用性
- `GET /check-email` - 检查邮箱可用性

### 头像管理 API (`/api/users`)
- `POST /{userId}/avatar/upload` - 上传头像
- `GET /{userId}/avatar` - 获取头像URL
- `DELETE /{userId}/avatar` - 删除头像

### 情侣管理 API (`/api/couples`)
- `GET /me` - 获取情侣详细信息
- `GET /{coupleId}/status` - 获取情侣状态
- `PUT /{coupleId}/status` - 更新情侣状态
- `GET /me/stats` - 获取情侣统计信息
- `GET /me/members` - 获取情侣成员信息
- `GET /me/duration` - 获取关系时长
- `GET /me/milestones` - 获取情侣里程碑

### 恋爱信息 API (`/api/couples`)
- `GET /my/love-info` - 获取恋爱信息
- `PUT /{coupleId}/love-start-date` - 更新恋爱开始日期

### 纪念日管理 API (`/api/anniversaries`)
- `GET /my` - 获取我的纪念日
- `POST /` - 创建纪念日
- `PUT /{id}` - 更新纪念日
- `DELETE /{id}` - 删除纪念日
- `GET /search` - 搜索纪念日
- `GET /couple/{coupleId}/upcoming/{days}` - 获取即将到来的纪念日
- `GET /couple/{coupleId}/overview` - 获取纪念日概览
- `GET /{id}/stats` - 获取纪念日统计
- `PUT /{id}/notification-toggle` - 切换推送状态
- `PUT /batch-notification` - 批量设置推送

### 待办事项管理 API (`/api/todos`)
- `POST /` - 创建待办事项
- `GET /couple/{coupleId}` - 获取情侣的所有待办事项
- `GET /couple/{coupleId}/status/{status}` - 根据状态获取待办事项
- `PUT /{todoId}` - 更新待办事项
- `PUT /{todoId}/complete` - 完成待办事项
- `DELETE /{todoId}` - 删除待办事项
- `GET /couple/{coupleId}/search` - 搜索待办事项
- `GET /couple/{coupleId}/stats` - 获取待办事项统计

### 系统管理 API
- `GET /api/health` - 健康检查
- `GET /api/health/detailed` - 详细健康检查
- `GET /api/health/memory` - 内存使用情况
- `GET /api/docs` - API文档概览
- `GET /api/docs/users` - 用户API文档
- `GET /api/docs/anniversaries` - 纪念日API文档

## 快速开始

### 环境要求
- JDK 17+
- Maven 3.6+

### 启动步骤

1. **克隆项目**
```bash
git clone <repository-url>
cd LoveDiary
```

2. **编译项目**
```bash
mvn clean compile
```

3. **启动应用**
```bash
mvn spring-boot:run
```

4. **访问应用**
- 应用地址: http://localhost:8080
- API文档: http://localhost:8080/api/docs
- 健康检查: http://localhost:8080/api/health

### 测试API

使用提供的测试脚本：
```bash
chmod +x test_api.sh
./test_api.sh
```

或使用curl手动测试：
```bash
# 健康检查
curl http://localhost:8080/api/health

# 用户注册
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456","nickname":"测试用户"}'

# 用户登录
curl -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"identifier":"test","password":"123456"}'
```

## 数据库设计

### 用户表 (users)
- id: 主键
- username: 用户名（唯一）
- password: 密码（加密存储）
- nickname: 昵称
- email: 邮箱
- phone: 手机号
- invitation_code: 邀请码（6位）
- status: 用户状态（SINGLE/MATCHED）
- couple_id: 情侣ID
- gender: 性别
- birth_date: 生日
- avatar_url: 头像URL
- created_at: 创建时间
- updated_at: 更新时间
- is_deleted: 软删除标记

### 情侣表 (couples)
- id: 主键
- user1_id: 用户1ID
- user2_id: 用户2ID
- love_start_date: 恋爱开始日期
- match_date: 匹配日期
- status: 情侣状态（ACTIVE/INACTIVE）
- created_at: 创建时间
- updated_at: 更新时间
- is_deleted: 软删除标记

### 纪念日表 (anniversaries)
- id: 主键
- name: 纪念日名称
- date: 纪念日日期
- description: 描述
- is_important: 是否重要
- enable_notification: 是否启用推送
- couple_id: 情侣ID
- created_at: 创建时间
- updated_at: 更新时间

## 认证机制

### JWT令牌
- 令牌有效期: 24小时
- 算法: HS256
- 包含信息: 用户ID、用户名、过期时间

### 权限控制
- 公开接口: 注册、登录、健康检查、API文档
- 认证接口: 需要JWT令牌，验证用户身份
- 权限验证: 只能操作自己的数据，情侣数据需要验证权限

## 错误处理

### 统一错误响应格式
```json
{
  "success": false,
  "message": "错误描述信息",
  "error": "错误类型",
  "timestamp": 1704096000000
}
```

### 常见HTTP状态码
- 200: 请求成功
- 400: 请求参数错误
- 401: 未授权，需要登录
- 403: 禁止访问，权限不足
- 404: 资源不存在
- 413: 文件大小超限
- 500: 服务器内部错误

## 部署说明

### 开发环境
- 数据库: H2内存数据库
- 文件存储: 本地文件系统
- 端口: 8080

### 生产环境建议
- 数据库: MySQL/PostgreSQL
- 文件存储: 云存储服务
- 反向代理: Nginx
- 容器化: Docker

## 开发计划

### 已完成功能
- ✅ 用户注册登录
- ✅ 情侣匹配
- ✅ 纪念日管理
- ✅ 头像上传
- ✅ API文档
- ✅ 健康检查

### 待开发功能
- 🔄 消息推送服务
- 🔄 数据统计报表
- 🔄 用户行为分析
- 🔄 多语言支持
- 🔄 移动端APP

## 贡献指南

1. Fork 项目
2. 创建功能分支
3. 提交更改
4. 推送到分支
5. 创建 Pull Request

## 许可证

MIT License

## 联系方式

- 项目地址: [GitHub Repository]
- 问题反馈: [Issues]
- 邮箱: [your-email@example.com]

---

**LoveDiary** - 让爱情更有仪式感 💕
