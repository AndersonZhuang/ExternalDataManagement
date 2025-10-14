package com.diit.ExternelDataManagement.controller;

import com.diit.ExternelDataManagement.common.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@Tag(name = "健康检查", description = "应用健康状态检查")
public class HealthController {

    @GetMapping
    @Operation(summary = "健康检查", description = "检查应用运行状态")
    public APIResponse<Map<String, Object>> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("timestamp", LocalDateTime.now());
        status.put("service", "ExternelDataManagement");
        status.put("version", "1.0.0");

        return APIResponse.ok(status);
    }
}