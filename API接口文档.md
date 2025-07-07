# LoveDiary API 接口文档

## 概述

LoveDiary 是一个情侣纪念日管理系统的后端API服务，提供用户管理、情侣匹配、纪念日管理等功能。

- **基础URL**: `http://localhost:8080/api`
- **认证方式**: JWT Bearer Token
- **数据格式**: JSON

## 认证说明

### JWT Token 格式
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 公开接口（无需认证）
- `POST /api/users/register` - 用户注册
- `POST /api/users/login` - 用户登录
- `GET /api/users/check-username` - 检查用户名可用性
- `GET /api/users/check-email` - 检查邮箱可用性
- `GET /api/health` - 健康检查
- `GET /api/docs` - API文档

## 1. 用户管理 API

### 1.1 用户注册
**POST** `/api/users/register`

**请求体**:
```json
{
  "username": "testuser",
  "password": "123456",
  "nickname": "测试用户",
  "email": "test@example.com",
  "gender": "MALE"
}
```

**响应**:
```json
{
  "success": true,
  "message": "注册成功",
  "user": {
    "id": 1,
    "username": "testuser",
    "nickname": "测试用户",
    "email": "test@example.com",
    "invitationCode": "ABC123",
    "status": "SINGLE",
    "gender": "MALE",
    "avatarUrl": "/images/default-male.png",
    "createdAt": "2024-01-01T10:00:00"
  }
}
```

### 1.2 用户登录
**POST** `/api/users/login`

**请求体**:
```json
{
  "identifier": "testuser",
  "password": "123456"
}
```

**响应**:
```json
{
  "success": true,
  "message": "登录成功",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "username": "testuser",
    "nickname": "测试用户",
    "email": "test@example.com",
    "invitationCode": "ABC123",
    "status": "SINGLE",
    "gender": "MALE",
    "avatarUrl": "/images/default-male.png"
  }
}
```

### 1.3 获取当前用户信息
**GET** `/api/users/me`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```json
{
  "success": true,
  "user": {
    "id": 1,
    "username": "testuser",
    "nickname": "测试用户",
    "email": "test@example.com",
    "invitationCode": "ABC123",
    "status": "MATCHED",
    "coupleId": 1,
    "gender": "MALE",
    "birthDate": "1990-01-01",
    "avatarUrl": "/images/default-male.png",
    "createdAt": "2024-01-01T10:00:00"
  }
}
```

### 1.4 更新用户资料
**PUT** `/api/users/{userId}/profile`

**请求头**:
```
Authorization: Bearer {token}
```

**请求体**:
```json
{
  "nickname": "新昵称",
  "email": "newemail@example.com",
  "phone": "13800138000",
  "gender": "FEMALE",
  "birthDate": "1995-05-05"
}
```

**响应**:
```json
{
  "success": true,
  "message": "资料更新成功",
  "user": {
    "id": 1,
    "username": "testuser",
    "nickname": "新昵称",
    "email": "newemail@example.com",
    "phone": "13800138000",
    "gender": "FEMALE",
    "birthDate": "1995-05-05"
  }
}
```

### 1.5 修改密码
**PUT** `/api/users/{userId}/password`

**请求头**:
```
Authorization: Bearer {token}
```

**请求体**:
```json
{
  "oldPassword": "123456",
  "newPassword": "newpassword123"
}
```

**响应**:
```json
{
  "success": true,
  "message": "密码修改成功"
}
```

### 1.6 通过邀请码匹配情侣
**POST** `/api/users/{userId}/match`

**请求头**:
```
Authorization: Bearer {token}
```

**请求体**:
```json
{
  "invitationCode": "XYZ789"
}
```

**响应**:
```json
{
  "success": true,
  "message": "匹配成功！",
  "couple": {
    "id": 1,
    "user1Id": 1,
    "user2Id": 2,
    "loveStartDate": "2024-01-01",
    "matchDate": "2024-01-01T10:30:00",
    "status": "ACTIVE",
    "createdAt": "2024-01-01T10:30:00"
  },
  "partner": {
    "id": 2,
    "username": "partner",
    "nickname": "另一半",
    "email": "partner@example.com",
    "gender": "FEMALE",
    "avatarUrl": "/images/default-female.png"
  }
}
```

### 1.7 获取邀请码
**GET** `/api/users/me/invitation-code`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```json
{
  "success": true,
  "invitationCode": "ABC123",
  "message": "您的邀请码是：ABC123"
}
```

### 1.8 获取用户统计信息
**GET** `/api/users/me/stats`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```json
{
  "success": true,
  "userId": 1,
  "username": "testuser",
  "nickname": "测试用户",
  "registrationDate": "2024-01-01T10:00:00",
  "isMatched": true,
  "coupleId": 1,
  "matchDate": "2024-01-01T10:30:00",
  "loveStartDate": "2024-01-01",
  "loveDays": 30
}
```

## 2. 头像管理 API

### 2.1 上传头像
**POST** `/api/users/{userId}/avatar/upload`

**请求头**:
```
Authorization: Bearer {token}
Content-Type: multipart/form-data
```

**请求体**:
```
file: [图片文件]
```

**响应**:
```json
{
  "success": true,
  "message": "头像上传成功",
  "avatarUrl": "/uploads/avatars/user1_avatar.jpg"
}
```

### 2.2 获取头像URL
**GET** `/api/users/{userId}/avatar`

**响应**:
```json
{
  "success": true,
  "avatarUrl": "/uploads/avatars/user1_avatar.jpg"
}
```

### 2.3 删除头像
**DELETE** `/api/users/{userId}/avatar`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```json
{
  "success": true,
  "message": "头像已删除，已恢复默认头像",
  "avatarUrl": "/images/default-male.png"
}
```

## 3. 情侣管理 API

### 3.1 获取情侣详细信息
**GET** `/api/couples/me`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```json
{
  "success": true,
  "couple": {
    "id": 1,
    "status": "ACTIVE",
    "loveStartDate": "2024-01-01",
    "matchDate": "2024-01-01T10:30:00",
    "createdAt": "2024-01-01T10:30:00",
    "user1": {
      "id": 1,
      "username": "user1",
      "nickname": "用户1",
      "email": "user1@example.com",
      "gender": "MALE",
      "avatarUrl": "/images/default-male.png"
    },
    "user2": {
      "id": 2,
      "username": "user2",
      "nickname": "用户2",
      "email": "user2@example.com",
      "gender": "FEMALE",
      "avatarUrl": "/images/default-female.png"
    }
  }
}
```

### 3.2 获取情侣统计信息
**GET** `/api/couples/me/stats`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```json
{
  "success": true,
  "stats": {
    "coupleId": 1,
    "status": "ACTIVE",
    "isActive": true,
    "matchDate": "2024-01-01T10:30:00",
    "loveStartDate": "2024-01-01",
    "matchDays": 30,
    "loveDays": 30
  }
}
```

### 3.3 获取关系时长
**GET** `/api/couples/me/duration`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```json
{
  "success": true,
  "duration": {
    "matchDays": 30,
    "matchWeeks": 4,
    "matchMonths": 1,
    "matchYears": 0,
    "loveDays": 30,
    "loveWeeks": 4,
    "loveMonths": 1,
    "loveYears": 0
  }
}
```

### 3.4 获取情侣里程碑
**GET** `/api/couples/me/milestones`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```json
{
  "success": true,
  "milestones": [
    {
      "type": "MATCH",
      "title": "成为情侣",
      "date": "2024-01-01T10:30:00",
      "description": "通过邀请码成功匹配"
    },
    {
      "type": "LOVE_START",
      "title": "恋爱开始",
      "date": "2024-01-01",
      "description": "正式确定恋爱关系"
    },
    {
      "type": "SPECIAL_DAY",
      "title": "30天纪念",
      "days": 30,
      "achieved": true,
      "description": "恋爱30天纪念"
    }
  ],
  "count": 3
}
```

## 4. 恋爱信息 API

### 4.1 获取恋爱信息
**GET** `/api/couples/my/love-info`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```json
{
  "success": true,
  "coupleId": 1,
  "loveStartDate": "2024-01-01",
  "loveDays": 30,
  "message": "我们已经恋爱30天了"
}
```

### 4.2 更新恋爱开始日期
**PUT** `/api/couples/{coupleId}/love-start-date`

**请求头**:
```
Authorization: Bearer {token}
```

**请求体**:
```json
{
  "loveStartDate": "2024-01-01"
}
```

**响应**:
```json
{
  "success": true,
  "message": "恋爱开始日期更新成功",
  "loveStartDate": "2024-01-01",
  "loveDays": 30
}
```

## 5. 纪念日管理 API

### 5.1 获取我的纪念日
**GET** `/api/anniversaries/my`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```json
[
  {
    "id": 1,
    "name": "恋爱纪念日",
    "date": "2024-01-01",
    "description": "我们开始恋爱的日子",
    "isImportant": true,
    "enableNotification": true,
    "coupleId": 1,
    "createdAt": "2024-01-01T10:30:00"
  }
]
```

### 5.2 创建纪念日
**POST** `/api/anniversaries`

**请求头**:
```
Authorization: Bearer {token}
```

**请求体**:
```json
{
  "name": "第一次约会",
  "date": "2024-01-15",
  "description": "我们第一次约会的美好回忆",
  "isImportant": true,
  "enableNotification": true
}
```

**响应**:
```json
{
  "id": 2,
  "name": "第一次约会",
  "date": "2024-01-15",
  "description": "我们第一次约会的美好回忆",
  "isImportant": true,
  "enableNotification": true,
  "coupleId": 1,
  "createdAt": "2024-01-01T11:00:00"
}
```

### 5.3 更新纪念日
**PUT** `/api/anniversaries/{id}`

**请求头**:
```
Authorization: Bearer {token}
```

**请求体**:
```json
{
  "name": "第一次约会纪念日",
  "date": "2024-01-15",
  "description": "我们第一次约会的美好回忆",
  "isImportant": true,
  "enableNotification": false
}
```

**响应**:
```json
{
  "id": 2,
  "name": "第一次约会纪念日",
  "date": "2024-01-15",
  "description": "我们第一次约会的美好回忆",
  "isImportant": true,
  "enableNotification": false,
  "coupleId": 1,
  "createdAt": "2024-01-01T11:00:00"
}
```

### 5.4 删除纪念日
**DELETE** `/api/anniversaries/{id}`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```
删除成功
```

### 5.5 搜索纪念日
**GET** `/api/anniversaries/search?keyword=约会`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```json
[
  {
    "id": 2,
    "name": "第一次约会纪念日",
    "date": "2024-01-15",
    "description": "我们第一次约会的美好回忆",
    "isImportant": true,
    "enableNotification": false,
    "coupleId": 1
  }
]
```

### 5.6 获取即将到来的纪念日
**GET** `/api/anniversaries/couple/{coupleId}/upcoming/{days}`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```json
[
  {
    "id": 2,
    "name": "第一次约会纪念日",
    "date": "2024-01-15",
    "description": "我们第一次约会的美好回忆",
    "isImportant": true,
    "enableNotification": false,
    "coupleId": 1,
    "daysUntil": 5
  }
]
```

### 5.7 获取纪念日概览
**GET** `/api/anniversaries/couple/{coupleId}/overview`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```json
{
  "today": [],
  "hasToday": false,
  "next7Days": [
    {
      "id": 2,
      "name": "第一次约会纪念日",
      "date": "2024-01-15",
      "daysUntil": 5
    }
  ],
  "next30Days": [
    {
      "id": 2,
      "name": "第一次约会纪念日",
      "date": "2024-01-15",
      "daysUntil": 5
    }
  ],
  "thisMonth": [
    {
      "id": 2,
      "name": "第一次约会纪念日",
      "date": "2024-01-15"
    }
  ]
}
```

### 5.8 切换推送状态
**PUT** `/api/anniversaries/{id}/notification-toggle`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```json
{
  "id": 2,
  "name": "第一次约会纪念日",
  "enableNotification": false,
  "message": "已禁用推送通知"
}
```

### 5.9 批量设置推送
**PUT** `/api/anniversaries/batch-notification`

**请求头**:
```
Authorization: Bearer {token}
```

**请求体**:
```json
{
  "anniversaryIds": [1, 2, 3],
  "enabled": true
}
```

**响应**:
```json
{
  "updatedCount": 3,
  "totalRequested": 3,
  "enabled": true,
  "message": "成功启用 3个纪念日的推送通知"
}
```

## 6. 系统管理 API

### 6.1 健康检查
**GET** `/api/health`

**响应**:
```json
{
  "status": "UP",
  "service": "LoveDiary API",
  "version": "1.0.0",
  "timestamp": "2024-01-01T10:00:00",
  "uptime": 1704096000000
}
```

### 6.2 详细健康检查
**GET** `/api/health/detailed`

**响应**:
```json
{
  "status": "UP",
  "service": "LoveDiary API",
  "version": "1.0.0",
  "timestamp": "2024-01-01T10:00:00",
  "system": {
    "javaVersion": "17.0.1",
    "osName": "Windows 10",
    "osVersion": "10.0",
    "availableProcessors": 8,
    "totalMemory": 268435456,
    "freeMemory": 134217728,
    "maxMemory": 1073741824
  },
  "components": {
    "database": "UP",
    "jwt": "UP",
    "fileStorage": "UP"
  }
}
```

### 6.3 内存使用情况
**GET** `/api/health/memory`

**响应**:
```json
{
  "totalMemory": 268435456,
  "freeMemory": 134217728,
  "usedMemory": 134217728,
  "maxMemory": 1073741824,
  "totalMemoryMB": 256,
  "freeMemoryMB": 128,
  "usedMemoryMB": 128,
  "maxMemoryMB": 1024,
  "usagePercentage": 50
}
```

## 7. API文档 API

### 7.1 获取API概览
**GET** `/api/docs`

**响应**:
```json
{
  "title": "LoveDiary API 接口文档",
  "version": "1.0.0",
  "description": "情侣纪念日管理系统的后端API接口",
  "baseUrl": "/api",
  "endpoints": {
    "users": {
      "register": "POST /api/users/register - 用户注册",
      "login": "POST /api/users/login - 用户登录",
      "profile": "GET /api/users/me - 获取当前用户信息"
    }
  }
}
```

### 7.2 获取用户API文档
**GET** `/api/docs/users`

### 7.3 获取纪念日API文档
**GET** `/api/docs/anniversaries`

### 7.4 获取认证说明
**GET** `/api/docs/auth`

### 7.5 获取错误码说明
**GET** `/api/docs/errors`

## 错误响应格式

所有API在发生错误时都会返回统一的错误格式：

```json
{
  "success": false,
  "message": "错误描述信息",
  "error": "错误类型",
  "timestamp": 1704096000000
}
```

### 常见HTTP状态码

- **200**: 请求成功
- **400**: 请求参数错误
- **401**: 未授权，需要登录
- **403**: 禁止访问，权限不足
- **404**: 资源不存在
- **413**: 文件大小超限
- **500**: 服务器内部错误

## 测试工具

### 使用curl测试

1. **用户注册**:
```bash
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "123456",
    "nickname": "测试用户",
    "email": "test@example.com"
  }'
```

2. **用户登录**:
```bash
curl -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "testuser",
    "password": "123456"
  }'
```

3. **获取用户信息**:
```bash
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 使用Postman测试

1. 导入以下环境变量：
   - `baseUrl`: `http://localhost:8080/api`
   - `token`: 登录后获取的JWT令牌

2. 在请求头中添加：
   - `Authorization`: `Bearer {{token}}`
   - `Content-Type`: `application/json`

## 注意事项

1. **JWT令牌有效期**: 24小时，过期后需要重新登录
2. **文件上传限制**: 头像文件大小不超过5MB，支持jpg、png、gif格式
3. **邀请码**: 6位字母数字组合，注册后自动生成
4. **情侣匹配**: 只能与单身用户匹配，匹配后双方状态变为MATCHED
5. **权限控制**: 只能操作自己的数据，情侣数据需要验证权限
6. **软删除**: 用户和情侣关系采用软删除，不会真正删除数据

## 更新日志

### v1.0.0 (2024-01-01)
- 初始版本发布
- 支持用户注册、登录、情侣匹配
- 支持纪念日管理
- 支持头像上传
- 支持JWT认证 