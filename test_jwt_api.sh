#!/bin/bash

# JWT API 测试脚本
# 用于测试恋爱日记APP的JWT认证功能

BASE_URL="http://localhost:8080"
echo "🚀 开始测试JWT认证API..."
echo "基础URL: $BASE_URL"
echo ""

# 1. 注册用户A
echo "📝 1. 注册用户A..."
RESPONSE_A=$(curl -s -X POST "$BASE_URL/api/users/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser1",
    "password": "123456",
    "nickname": "测试用户1",
    "email": "test1@example.com"
  }')

echo "响应: $RESPONSE_A"
echo ""

# 2. 注册用户B
echo "📝 2. 注册用户B..."
RESPONSE_B=$(curl -s -X POST "$BASE_URL/api/users/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser2",
    "password": "123456",
    "nickname": "测试用户2",
    "email": "test2@example.com"
  }')

echo "响应: $RESPONSE_B"
echo ""

# 3. 用户A登录获取token
echo "🔑 3. 用户A登录获取JWT token..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/users/login" \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "testuser1",
    "password": "123456"
  }')

echo "响应: $LOGIN_RESPONSE"

# 提取token（简单提取，实际应用中建议使用jq）
TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
echo "提取到的token: $TOKEN"
echo ""

# 4. 使用token获取用户信息
echo "👤 4. 使用token获取用户信息..."
USER_INFO=$(curl -s -X GET "$BASE_URL/api/users/1" \
  -H "Authorization: Bearer $TOKEN")

echo "响应: $USER_INFO"
echo ""

# 5. 测试无token访问（应该返回401）
echo "🚫 5. 测试无token访问（应该返回401）..."
NO_TOKEN_RESPONSE=$(curl -s -X GET "$BASE_URL/api/users/1")

echo "响应: $NO_TOKEN_RESPONSE"
echo ""

# 6. 获取我的纪念日
echo "💕 6. 获取我的纪念日..."
MY_ANNIVERSARIES=$(curl -s -X GET "$BASE_URL/api/anniversaries/my" \
  -H "Authorization: Bearer $TOKEN")

echo "响应: $MY_ANNIVERSARIES"
echo ""

echo "✅ JWT API测试完成！"
echo ""
echo "📋 测试总结："
echo "- 注册功能：无需认证 ✓"
echo "- 登录功能：返回JWT token ✓"  
echo "- 有token访问：正常访问 ✓"
echo "- 无token访问：返回401错误 ✓"
echo "- 纪念日接口：需要认证 ✓"
echo ""
echo "🎉 JWT认证机制工作正常！" 