package com.lance.controller;

import com.lance.service.DoctorWorkbenchService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * MCP Controller for handling MCP-related endpoints
 * 
 * @author lance
 */
@RestController
@RequestMapping("/api/mcp")
public class McpController {

    @Resource
    private DoctorWorkbenchService doctorWorkbenchService;


    @GetMapping("/doctor/workbench/bind")
    public Object getBindDoctorId(Long patientId) {
        return doctorWorkbenchService.getBindDoctorId(patientId);
    }
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "running");
        status.put("service", "MCP Server");
        status.put("version", "1.0.0");
        status.put("timestamp", System.currentTimeMillis());
        return status;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "MCP Server");
        return health;
    }
}
