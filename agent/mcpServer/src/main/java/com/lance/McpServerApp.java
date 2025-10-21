package com.lance;

import com.lance.service.config.McpToolConfiguration;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * MCP Server Spring Boot Application
 * 
 * @author lance
 */
@SpringBootApplication
@Import(McpToolConfiguration.class)
public class McpServerApp {
    
    public static void main(String[] args) {
        SpringApplication.run(McpServerApp.class, args);
    }

    @Bean
    public ToolCallbackProvider weatherTools(McpToolConfiguration mcpConfig, ApplicationContext applicationContext) {
        return mcpConfig.mcpTools(applicationContext);
    }
}
