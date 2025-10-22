package com.lance;

import com.lance.service.config.McpToolConfiguration;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
//import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * MCP Server Spring Boot Application
 * 
 * @author lance
 */
@SpringBootApplication
@EnableDiscoveryClient
//@EnableFeignClients(basePackages = {
//        "com.lance.service.feign.**"
//})
@Import(McpToolConfiguration.class)
public class McpServerApp {
    
    public static void main(String[] args) {
        SpringApplication.run(McpServerApp.class, args);
    }

    @Bean
    public ToolCallbackProvider weatherTools(McpToolConfiguration mcpConfig, ApplicationContext applicationContext) {
        return mcpConfig.mcpTools(applicationContext);
    }
    public record TextInput(String input) {
    }
    @Bean
    public ToolCallback toUpperCase() {
        return FunctionToolCallback.builder("toUpperCase", (TextInput input) -> input.input().toUpperCase())
                .inputType(TextInput.class)
                .description("Put the text to upper case")
                .build();
    }
}
