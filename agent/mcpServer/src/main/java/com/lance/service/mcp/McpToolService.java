package com.lance.service.mcp;

import com.lance.annotation.McpTool;
import org.springframework.stereotype.Service;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * MCP工具服务示例
 * 
 * @author lance
 */
@Service
@McpTool("mcp-tool-service")
public class McpToolService {

    @Tool(description = "获取当前系统时间")
    public String getCurrentTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return "当前时间: " + now.format(formatter);
    }

    @Tool(description = "计算两个数字的和")
    public String addNumbers(
            @ToolParam(required = true, description = "第一个数字") double num1,
            @ToolParam(required = true, description = "第二个数字") double num2) {
        double result = num1 + num2;
        return String.format("%.2f + %.2f = %.2f", num1, num2, result);
    }

    @Tool(description = "获取系统信息")
    public Map<String, Object> getSystemInfo() {
        Map<String, Object> systemInfo = new HashMap<>();
        systemInfo.put("os", System.getProperty("os.name"));
        systemInfo.put("javaVersion", System.getProperty("java.version"));
        systemInfo.put("user", System.getProperty("user.name"));
        systemInfo.put("timestamp", System.currentTimeMillis());
        return systemInfo;
    }

    @Tool(description = "生成随机字符串")
    public String generateRandomString(
            @ToolParam(required = true, description = "字符串长度") int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            result.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return "生成的随机字符串: " + result.toString();
    }
}
