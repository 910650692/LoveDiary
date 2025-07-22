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

## 1. 生理期记录 API

### 1.1 创建生理期记录
**POST** `/api/period-records`

**认证**: 需要JWT Token

**请求体**:
```json
{
  "startDate": "2024-01-01",
  "endDate": "2024-01-05"
}
```

**响应**:
```json
{
  "success": true,
  "message": "生理期记录创建成功",
  "data": {
    "id": 1,
    "userId": 1,
    "coupleId": 1,
    "startDate": "2024-01-01",
    "endDate": "2024-01-05",
    "cycleLength": null,
    "isPredicted": false,
    "createdAt": "2024-01-01T10:00:00"
  }
}
```

### 1.2 更新生理期记录
**PUT** `/api/period-records/{id}`

**认证**: 需要JWT Token

**请求体**:
```json
{
  "startDate": "2024-01-01",
  "endDate": "2024-01-06"
}
```

**响应**:
```json
{
  "success": true,
  "message": "生理期记录更新成功",
  "data": {
    "id": 1,
    "startDate": "2024-01-01",
    "endDate": "2024-01-06",
    "cycleLength": 28
  }
}
```

### 1.3 删除生理期记录
**DELETE** `/api/period-records/{id}`

**认证**: 需要JWT Token

**响应**:
```json
{
  "success": true,
  "message": "生理期记录删除成功"
}
```

### 1.4 获取生理期记录列表
**GET** `/api/period-records`

**认证**: 需要JWT Token

**查询参数**:
- `startDate` (可选): 开始日期，格式：YYYY-MM-DD
- `endDate` (可选): 结束日期，格式：YYYY-MM-DD  
- `isPredicted` (可选): 是否预测记录，true/false

**响应**:
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "startDate": "2024-01-01",
      "endDate": "2024-01-05",
      "cycleLength": 28,
      "isPredicted": false
    }
  ],
  "count": 1
}
```

### 1.5 获取生理期预测信息
**GET** `/api/period-records/prediction`

**认证**: 需要JWT Token

**响应**:
```json
{
  "success": true,
  "data": {
    "canPredict": true,
    "nextStartDate": "2024-01-29",
    "predictedStartRange": {
      "earliest": "2024-01-27",
      "latest": "2024-01-31"
    },
    "averageCycleLength": 28,
    "cycleRegularity": "规律",
    "basedOnCycles": 3
  }
}
```

### 1.6 生成预测记录
**POST** `/api/period-records/generate-predictions`

**认证**: 需要JWT Token

**响应**:
```json
{
  "success": true,
  "message": "预测记录生成成功"
}
```

### 1.7 获取用户统计信息
**GET** `/api/period-records/statistics`

**认证**: 需要JWT Token

**响应**:
```json
{
  "success": true,
  "data": {
    "totalActualRecords": 5,
    "totalPredictedRecords": 3,
    "canPredict": true,
    "lastRecordDate": "2024-01-01"
  }
}
```

## 2. 用户管理 API

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

## 8. 待办事项管理 API（MVP版本）

### 8.1 创建待办事项
**POST** `/api/todos`

**请求头**:
```
Authorization: Bearer {token}
Content-Type: application/json
```

**请求体**:
```json
{
  "coupleId": 1,
  "title": "买生日礼物",
  "description": "为另一半准备生日礼物"
}
```

**响应**:
```json
{
  "success": true,
  "message": "待办事项创建成功",
  "todoItem": {
    "id": 1,
    "coupleId": 1,
    "creatorId": 1,
    "completerId": null,
    "title": "买生日礼物",
    "description": "为另一半准备生日礼物",
    "status": "PENDING",
    "completedAt": null,
    "createdAt": "2024-01-01T10:30:00",
    "updatedAt": "2024-01-01T10:30:00",
    "isCompleted": false
  }
}
```

### 8.2 获取情侣的所有待办事项
**GET** `/api/todos/couple/{coupleId}`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```json
{
  "success": true,
  "todoItems": [
    {
      "id": 1,
      "coupleId": 1,
      "creatorId": 1,
      "completerId": null,
      "title": "买生日礼物",
      "description": "为另一半准备生日礼物",
      "priority": "HIGH",
      "status": "PENDING",
      "dueDate": "2024-01-15T18:00:00",
      "completedAt": null,
      "tags": ["礼物", "生日"],
      "isImportant": true,
      "createdAt": "2024-01-01T10:30:00",
      "updatedAt": "2024-01-01T10:30:00",
      "isCompleted": false,
      "isOverdue": false,
      "isDueSoon": false
    }
  ],
  "count": 1
}
```

### 8.3 根据状态获取待办事项
**GET** `/api/todos/couple/{coupleId}/status/{status}`

**请求头**:
```
Authorization: Bearer {token}
```

**路径参数**:
- `status`: 状态值（PENDING, COMPLETED）

**响应**:
```json
{
  "success": true,
  "todoItems": [...],
  "count": 1,
  "status": "PENDING"
}
```

### 8.4 更新待办事项
**PUT** `/api/todos/{todoId}`

**请求头**:
```
Authorization: Bearer {token}
Content-Type: application/json
```

**请求体**:
```json
{
  "title": "更新后的标题",
  "description": "更新后的描述"
}
```

**响应**:
```json
{
  "success": true,
  "message": "待办事项更新成功",
  "todoItem": {...}
}
```

### 8.5 完成待办事项
**PUT** `/api/todos/{todoId}/complete`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```json
{
  "success": true,
  "message": "待办事项已完成",
  "todoItem": {
    "id": 1,
    "status": "COMPLETED",
    "completerId": 1,
    "completedAt": "2024-01-01T11:00:00",
    "isCompleted": true
  }
}
```

### 8.6 删除待办事项
**DELETE** `/api/todos/{todoId}`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```json
{
  "success": true,
  "message": "待办事项已删除"
}
```

### 8.7 搜索待办事项
**GET** `/api/todos/couple/{coupleId}/search?keyword=关键词`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```json
{
  "success": true,
  "todoItems": [...],
  "count": 1,
  "keyword": "关键词"
}
```

### 8.8 获取待办事项统计信息
**GET** `/api/todos/couple/{coupleId}/stats`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```json
{
  "success": true,
  "stats": {
    "total": 10,
    "completed": 5,
    "pending": 5,
    "completionRate": 50.0
  }
}
```

## 9. 相册管理 API

### 9.1 创建相册
**POST** `/api/albums`

**请求头**:
```
Authorization: Bearer {token}
Content-Type: application/json
```

**请求体**:
```json
{
  "coupleId": 1,
  "name": "我们的第一次约会",
  "description": "记录我们第一次约会的美好时光"
}
```

**响应**:
```json
{
  "success": true,
  "message": "相册创建成功",
  "album": {
    "id": 1,
    "coupleId": 1,
    "name": "我们的第一次约会",
    "description": "记录我们第一次约会的美好时光",
    "coverPhotoId": null,
    "photoCount": 0,
    "createdAt": "2024-01-01T10:30:00",
    "updatedAt": "2024-01-01T10:30:00"
  }
}
```

### 9.2 获取情侣的所有相册
**GET** `/api/albums?coupleId={coupleId}&userId={userId}`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```json
{
  "success": true,
  "albums": [
    {
      "id": 1,
      "coupleId": 1,
      "name": "我们的第一次约会",
      "description": "记录我们第一次约会的美好时光",
      "coverPhotoId": null,
      "photoCount": 0,
      "createdAt": "2024-01-01T10:30:00",
      "updatedAt": "2024-01-01T10:30:00"
    }
  ],
  "count": 1
}
```

### 9.3 获取相册详情
**GET** `/api/albums/{albumId}?userId={userId}`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```json
{
  "success": true,
  "album": {
    "id": 1,
    "coupleId": 1,
    "name": "我们的第一次约会",
    "description": "记录我们第一次约会的美好时光",
    "coverPhotoId": null,
    "photoCount": 0,
    "createdAt": "2024-01-01T10:30:00",
    "updatedAt": "2024-01-01T10:30:00"
  }
}
```

### 9.4 更新相册
**PUT** `/api/albums/{albumId}?userId={userId}`

**请求头**:
```
Authorization: Bearer {token}
Content-Type: application/json
```

**请求体**:
```json
{
  "name": "我们的第一次约会（更新）",
  "description": "记录我们第一次约会的美好时光 - 已更新"
}
```

**响应**:
```json
{
  "success": true,
  "message": "相册更新成功",
  "album": {
    "id": 1,
    "coupleId": 1,
    "name": "我们的第一次约会（更新）",
    "description": "记录我们第一次约会的美好时光 - 已更新",
    "coverPhotoId": null,
    "photoCount": 0,
    "createdAt": "2024-01-01T10:30:00",
    "updatedAt": "2024-01-01T11:00:00"
  }
}
```

### 9.5 搜索相册
**GET** `/api/albums/search?coupleId={coupleId}&userId={userId}&keyword=约会`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```json
{
  "success": true,
  "albums": [...],
  "count": 1,
  "keyword": "约会"
}
```

### 9.6 获取相册统计信息
**GET** `/api/albums/stats?coupleId={coupleId}&userId={userId}`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```json
{
  "success": true,
  "stats": {
    "totalAlbums": 5,
    "totalPhotos": 50,
    "totalVideos": 10,
    "totalSize": 104857600
  }
}
```

### 9.7 删除相册
**DELETE** `/api/albums/{albumId}?userId={userId}`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```json
{
  "success": true,
  "message": "相册删除成功"
}
```

## 10. 照片管理 API

### 10.1 上传照片
**POST** `/api/photo-upload`

**请求头**:
```
Authorization: Bearer {token}
Content-Type: multipart/form-data
```

**请求体**:
```
coupleId: 1
creatorId: 1
albumId: 1 (可选)
file: [图片或视频文件]
description: 我们的第一张合照 (可选)
location: 北京 (可选)
```

**响应**:
```json
{
  "success": true,
  "message": "照片上传成功",
  "photo": {
    "id": 1,
    "coupleId": 1,
    "albumId": 1,
    "creatorId": 1,
    "fileName": "photos/1/1/abc123.jpg",
    "originalName": "IMG_1234.JPG",
    "filePath": "photos/1/1/abc123.jpg",
    "fileSize": 1048576,
    "fileType": "PHOTO",
    "mimeType": "image/jpeg",
    "width": 1920,
    "height": 1080,
    "duration": null,
    "description": "我们的第一张合照",
    "location": "北京",
    "isFavorite": false,
    "tagIds": [1, 2, 3],
    "createdAt": "2024-01-01T10:30:00",
    "updatedAt": "2024-01-01T10:30:00",
    "fileUrl": "/files/photos/1/1/abc123.jpg",
    "fileTypeDescription": "JPEG图片"
  }
}
```

### 10.2 获取情侣的所有照片
**GET** `/api/photos?coupleId={coupleId}&userId={userId}`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```json
{
  "success": true,
  "photos": [...],
  "count": 10
}
```

### 10.3 获取指定相册的照片
**GET** `/api/photos?coupleId={coupleId}&userId={userId}&albumId={albumId}`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```json
{
  "success": true,
  "photos": [...],
  "count": 5
}
```

### 10.4 获取收藏的照片
**GET** `/api/photos/favorites?coupleId={coupleId}&userId={userId}`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```json
{
  "success": true,
  "photos": [...],
  "count": 3
}
```

### 10.5 获取照片详情
**GET** `/api/photos/{photoId}?userId={userId}`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```json
{
  "success": true,
  "photo": {
    "id": 1,
    "coupleId": 1,
    "albumId": 1,
    "creatorId": 1,
    "fileName": "photos/1/1/abc123.jpg",
    "originalName": "IMG_1234.JPG",
    "filePath": "photos/1/1/abc123.jpg",
    "fileSize": 1048576,
    "fileType": "PHOTO",
    "mimeType": "image/jpeg",
    "width": 1920,
    "height": 1080,
    "duration": null,
    "description": "我们的第一张合照",
    "location": "北京",
    "isFavorite": false,
    "tagIds": [1, 2, 3],
    "createdAt": "2024-01-01T10:30:00",
    "updatedAt": "2024-01-01T10:30:00",
    "fileUrl": "/files/photos/1/1/abc123.jpg",
    "fileTypeDescription": "JPEG图片"
  }
}
```

### 10.6 更新照片信息
**PUT** `/api/photos/{photoId}?userId={userId}`

**请求头**:
```
Authorization: Bearer {token}
Content-Type: application/json
```

**请求体**:
```json
{
  "description": "我们的第一张合照（更新）",
  "location": "北京天安门",
  "isFavorite": true
}
```

**响应**:
```json
{
  "success": true,
  "message": "照片更新成功",
  "photo": {...}
}
```

### 10.7 切换收藏状态
**POST** `/api/photos/{photoId}/favorite?userId={userId}`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```json
{
  "success": true,
  "message": "已添加到收藏",
  "photo": {
    "id": 1,
    "isFavorite": true
  }
}
```

### 10.8 搜索照片
**GET** `/api/photos/search?coupleId={coupleId}&userId={userId}&keyword=合照`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```json
{
  "success": true,
  "photos": [...],
  "count": 2,
  "keyword": "合照"
}
```

### 10.9 删除照片
**DELETE** `/api/photos/{photoId}?userId={userId}`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```json
{
  "success": true,
  "message": "照片删除成功"
}
```

## 11. 标签管理 API

### 11.1 创建标签
**POST** `/api/tags?coupleId={coupleId}&userId={userId}&name=旅行&color=#FF6B6B`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```json
{
  "success": true,
  "message": "标签创建成功",
  "tag": {
    "id": 1,
    "coupleId": 1,
    "name": "旅行",
    "color": "#FF6B6B",
    "createdBy": 1,
    "createdAt": "2024-01-01T10:30:00"
  }
}
```

### 11.2 获取情侣的所有标签
**GET** `/api/tags?coupleId={coupleId}&userId={userId}`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```json
{
  "success": true,
  "tags": [
    {
      "id": 1,
      "coupleId": 1,
      "name": "旅行",
      "color": "#FF6B6B",
      "createdBy": 1,
      "createdAt": "2024-01-01T10:30:00"
    }
  ],
  "count": 1
}
```

### 11.3 更新标签
**PUT** `/api/tags/{tagId}?userId={userId}&name=旅行回忆&color=#FF8E53`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```json
{
  "success": true,
  "message": "标签更新成功",
  "tag": {
    "id": 1,
    "coupleId": 1,
    "name": "旅行回忆",
    "color": "#FF8E53",
    "createdBy": 1,
    "createdAt": "2024-01-01T10:30:00"
  }
}
```

### 11.4 删除标签
**DELETE** `/api/tags/{tagId}?userId={userId}`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```json
{
  "success": true,
  "message": "标签删除成功"
}
```

### 11.5 为照片分配标签
**POST** `/api/tags/assign?photoId={photoId}&userId={userId}`

**请求头**:
```
Authorization: Bearer {token}
Content-Type: application/json
```

**请求体**:
```json
[1, 2, 3]
```

**响应**:
```json
{
  "success": true,
  "message": "标签分配成功"
}
```

## 标签管理功能测试示例

### 测试场景1：创建标签
```bash
# 创建旅行标签
curl -X POST "http://localhost:8080/api/tags?coupleId=1&userId=1&name=旅行&color=%23FF6B6B" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# 创建美食标签
curl -X POST "http://localhost:8080/api/tags?coupleId=1&userId=1&name=美食&color=%234ECDC4" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# 创建纪念日标签
curl -X POST "http://localhost:8080/api/tags?coupleId=1&userId=1&name=纪念日&color=%2345B7D1" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 测试场景2：获取标签列表
```bash
curl -X GET "http://localhost:8080/api/tags?coupleId=1&userId=1" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 测试场景3：更新标签
```bash
curl -X PUT "http://localhost:8080/api/tags/1?userId=1&name=旅行回忆&color=%23FF8E53" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 测试场景4：为照片分配标签
```bash
curl -X POST "http://localhost:8080/api/tags/assign?photoId=1&userId=1" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d "[1, 2, 3]"
```

### 测试场景5：删除标签
```bash
curl -X DELETE "http://localhost:8080/api/tags/4?userId=1" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 测试场景6：错误处理测试
```bash
# 测试标签名重复（应该失败）
curl -X POST "http://localhost:8080/api/tags?coupleId=1&userId=1&name=旅行回忆&color=%23FF0000" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# 测试空标签名（应该失败）
curl -X POST "http://localhost:8080/api/tags?coupleId=1&userId=1&name=&color=%23FF0000" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Postman测试集合

在Postman中可以创建以下测试请求：

1. **创建标签**
   - Method: POST
   - URL: `http://localhost:8080/api/tags?coupleId=1&userId=1&name=旅行&color=#FF6B6B`
   - Headers: `Authorization: Bearer {{token}}`

2. **获取标签列表**
   - Method: GET
   - URL: `http://localhost:8080/api/tags?coupleId=1&userId=1`
   - Headers: `Authorization: Bearer {{token}}`

3. **更新标签**
   - Method: PUT
   - URL: `http://localhost:8080/api/tags/1?userId=1&name=旅行回忆&color=#FF8E53`
   - Headers: `Authorization: Bearer {{token}}`

4. **分配标签**
   - Method: POST
   - URL: `http://localhost:8080/api/tags/assign?photoId=1&userId=1`
   - Headers: `Authorization: Bearer {{token}}`
   - Body: `[1, 2, 3]`

5. **删除标签**
   - Method: DELETE
   - URL: `http://localhost:8080/api/tags/4?userId=1`
   - Headers: `Authorization: Bearer {{token}}`

## 12. 文件访问 API

### 12.1 下载文件
**GET** `/files/{filePath}`

**响应**:
```
文件二进制内容
```

### 12.2 获取文件信息
**GET** `/files/info/{filePath}`

**响应**:
```json
{
  "success": true,
  "fileInfo": {
    "fileName": "abc123.jpg",
    "filePath": "photos/1/1/abc123.jpg",
    "fileSize": 1048576,
    "mimeType": "image/jpeg",
    "exists": true,
    "url": "/files/photos/1/1/abc123.jpg"
  }
}
```

## 更新日志

### v1.2.0 (2024-01-03)
- 新增相册管理功能
- 新增照片和视频上传功能
- 新增标签管理功能
- 支持iOS格式（LIVP实况图片、HEIC图片）
- 支持文件本地存储和访问
- 支持照片收藏和搜索功能

### v1.1.0 (2024-01-02)
- 新增待办事项管理功能
- 支持情侣共享待办事项
- 支持优先级、状态、标签管理
- 支持截止日期和提醒功能
- 支持统计和概览功能

### v1.0.0 (2024-01-01)
- 初始版本发布
- 支持用户注册、登录、情侣匹配
- 支持纪念日管理
- 支持头像上传
- 支持JWT认证 