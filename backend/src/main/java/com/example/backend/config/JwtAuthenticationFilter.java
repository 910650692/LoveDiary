package com.example.backend.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * JWT认证过滤器
 * 验证API请求中的JWT token
 */
@Component
public class JwtAuthenticationFilter implements Filter {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String requestPath = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();
        
        // 跳过不需要认证的接口
        if (isPublicEndpoint(requestPath, method)) {
            chain.doFilter(request, response);
            return;
        }
        
        // 获取Authorization头
        String authHeader = httpRequest.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendErrorResponse(httpResponse, "Missing or invalid Authorization header");
            return;
        }
        
        // 提取token
        String token = authHeader.substring(7); // 去掉 "Bearer " 前缀
        
        // 验证token
        if (!jwtUtil.validateToken(token)) {
            sendErrorResponse(httpResponse, "Invalid or expired token");
            return;
        }
        
        // token有效，将用户信息添加到请求中
        Long userId = jwtUtil.getUserIdFromToken(token);
        String username = jwtUtil.getUsernameFromToken(token);
        
        if (userId != null && username != null) {
            httpRequest.setAttribute("userId", userId);
            httpRequest.setAttribute("username", username);
            chain.doFilter(request, response);
        } else {
            sendErrorResponse(httpResponse, "Invalid token data");
        }
    }
    
    /**
     * 检查是否是公开接口（不需要认证）
     */
    private boolean isPublicEndpoint(String path, String method) {
        // 用户注册和登录接口
        if (path.equals("/api/users/register") && method.equals("POST")) {
            return true;
        }
        if (path.equals("/api/users/login") && method.equals("POST")) {
            return true;
        }
        
        // 其他公开接口（如健康检查、文档等）
        if (path.startsWith("/actuator/") || path.startsWith("/swagger-") || path.startsWith("/v3/api-docs")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 发送错误响应
     */
    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        
        PrintWriter writer = response.getWriter();
        writer.write(String.format("{\"success\": false, \"message\": \"%s\", \"code\": 401}", message));
        writer.flush();
    }
} 