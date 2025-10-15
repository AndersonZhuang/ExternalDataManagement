package com.diit.ExternelDataManagement.service.impl;

import com.diit.ExternelDataManagement.config.SchedulerConfig;
import com.diit.ExternelDataManagement.service.WorkflowService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class WorkflowServiceImpl implements WorkflowService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowServiceImpl.class);

    @Autowired
    private SchedulerConfig schedulerConfig;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Long startWorkflow() {
        return startWorkflow(null);
    }

    @Override
    public Long startWorkflow(String initParams) {
        try {
            // 记录配置信息
            logger.info("调度服务配置信息:");
            logger.info("  Base URL: {}", schedulerConfig.getBaseUrl());
            logger.info("  Workflow ID: {}", schedulerConfig.getWorkflowId());
            logger.info("  Init Params: {}", initParams != null ? initParams : "未提供");

            // 构建完整URL
            String url = schedulerConfig.getBaseUrl() + "/api/workflow/" + schedulerConfig.getWorkflowId() + "/run";
            logger.info("构建的完整URL: {}", url);

            // 准备请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            logger.debug("请求头设置: Content-Type = application/json");

            // 准备请求体
            String requestBody = buildRequestBody(initParams);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            logger.debug("请求体: {}", requestBody);

            // 发送HTTP请求
            logger.info("发送POST请求到: {}", url);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            // 记录响应信息
            logger.info("响应状态码: {}", response.getStatusCode());
            logger.info("响应体: {}", response.getBody());

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                JsonNode dataNode = jsonNode.get("data");
                if (dataNode != null && !dataNode.isNull()) {
                    Long instanceId = dataNode.asLong();
                    logger.info("成功获取实例ID: {}", instanceId);
                    return instanceId;
                } else {
                    logger.warn("响应中未找到data字段或data字段为null");
                }
            } else {
                logger.error("API调用失败，状态码: {}", response.getStatusCode());
            }

            logger.warn("工作流启动失败，返回null");
            return null;
        } catch (Exception e) {
            logger.error("工作流API调用异常: ", e);
            logger.error("异常详细信息: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 构建请求体
     * @param initParams 初始化参数，允许为null
     * @return JSON格式的请求体字符串
     */
    private String buildRequestBody(String initParams) {
        try {
            ObjectNode requestJson = objectMapper.createObjectNode();
            
            if (initParams != null && !initParams.trim().isEmpty()) {
                requestJson.put("initParams", initParams);
                logger.debug("添加initParams到请求体: {}", initParams);
            } else {
                requestJson.put("initParams", "");
                logger.debug("initParams为空，使用默认空字符串");
            }
            
            return objectMapper.writeValueAsString(requestJson);
        } catch (Exception e) {
            logger.error("构建请求体异常: ", e);
            return "{\"initParams\": \"\"}";
        }
    }
}