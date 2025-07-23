# JWT认证API使用说明

## 🔐 JWT认证机制已经集成

现在所有API接口都需要JWT认证，只有登录和注册接口是公开的。

## 📝 API使用流程

### 1. 用户注册（无需认证）
```bash
POST /api/users/register
Content-Type: application/json

{
  "username": "testuser",
  "password": "123456",
  "nickname": "测试用户",
  "email": "test@example.com"
}
```

**响应：**
```json
{
  "success": true,
  "message": "注册成功",
  "user": {
    "id": 1,
    "username": "testuser",
    "nickname": "测试用户",
    "invitationCode": "ABC123",
    "status": "SINGLE"
  }
}
```

### 2. 用户登录（无需认证）
```bash
POST /api/users/login
Content-Type: application/json

{
  "identifier": "testuser",
  "password": "123456"
}
```

**响应：**
```json
{
  "success": true,
  "message": "登录成功",
  "token": "eyJhbGciOiJIUzUxMiJ9.eyJ1c2VySWQiOjEsInVzZXJuYW1lIjoidGVzdHVzZXIiLCJzdWIiOiJ0ZXN0dXNlciIsImlhdCI6MTcwNDM2NDgwMCwiZXhwIjoxNzA0OTY5NjAwfQ.signature",
  "user": {
    "id": 1,
    "username": "testuser",
    "invitationCode": "ABC123"
  }
}
```

### 3. 使用JWT token访问需要认证的API

**重要：** 登录后获得的`token`必须在后续所有API请求中携带！

#### 获取用户信息
```bash
GET /api/users/1
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJ1c2VySWQiOjEsInVzZXJuYW1lIjoidGVzdHVzZXIi...
```

#### 通过邀请码匹配情侣
```bash
POST /api/users/1/match
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJ1c2VySWQiOjEsInVzZXJuYW1lIjoidGVzdHVzZXIi...
Content-Type: application/json

{
  "invitationCode": "DEF456"
}
```

#### 获取我的纪念日
```bash
GET /api/anniversaries/my
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJ1c2VySWQiOjEsInVzZXJuYW1lIjoidGVzdHVzZXIi...
```

#### 创建纪念日
```bash
POST /api/anniversaries
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJ1c2VySWQiOjEsInVzZXJuYW1lIjoidGVzdHVzZXIi...
Content-Type: application/json

{
  "name": "初次相遇",
  "date": "2023-05-20",
  "notes": "在咖啡厅第一次见面",
  "coupleId": 1,
  "enableNotification": true
}
```

## ⚡ JWT Token特点

- **过期时间：** 7天
- **自动验证：** 所有`/api/*`接口都会自动验证token
- **用户信息：** token中包含userId和username，可以在Controller中通过`request.getAttribute("userId")`获取

## 🚫 无认证访问会返回401

如果访问需要认证的API时没有携带有效token：

```json
{
  "success": false,
  "message": "Missing or invalid Authorization header",
  "code": 401
}
```

## 🔄 完整的用户流程示例

1. **注册用户A** → 获得邀请码 `ABC123`
2. **注册用户B** → 获得邀请码 `DEF456`
3. **用户A登录** → 获得JWT token
4. **用户A使用token匹配用户B** → 输入邀请码`DEF456`
5. **匹配成功** → 两人成为情侣，可以共享纪念日数据
6. **使用token访问纪念日功能** → 创建、查看、编辑纪念日

## 🛡️ 安全保护

- 密码使用SHA-256加密存储
- JWT token有过期时间限制
- API接口权限验证
- 情侣数据隔离保护

现在你的恋爱日记APP已经有了完整的用户认证和权限保护机制！ 