package com.diit.ExternelDataManagement.service.impl;

import com.diit.ExternelDataManagement.config.SchedulerConfig;
import com.diit.ExternelDataManagement.exception.DataNotFoundException;
import com.diit.ExternelDataManagement.mapper.DataGovernanceMapper;
import com.diit.ExternelDataManagement.mapper.FileMapper;
import com.diit.ExternelDataManagement.pojo.DataGovernanceEntity;
import com.diit.ExternelDataManagement.pojo.SyncStatusResult;
import com.diit.ExternelDataManagement.pojo.WorkflowInstanceResponse;
import com.diit.ExternelDataManagement.service.GovernanceService;
import com.diit.ExternelDataManagement.service.WorkflowService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GovernanceServiceImpl implements GovernanceService {

    private static final Logger logger = LoggerFactory.getLogger(GovernanceServiceImpl.class);

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private DataGovernanceMapper dataGovernanceMapper;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private SchedulerConfig schedulerConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Object startGovernanceTask(String id, int workflowId) {
        logger.info("启动治理任务，治理记录ID: {}, 工作流ID: {}", id, workflowId);
        
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("治理记录ID不能为空");
        }
        
        // 1. 检查data_governance_info表中对应记录是否存在
        DataGovernanceEntity governanceEntity = dataGovernanceMapper.findById(id);
        if (governanceEntity == null) {
            logger.error("治理记录不存在，ID: {}", id);
            throw new DataNotFoundException("治理记录不存在，ID: " + id);
        }
        logger.info("治理记录存在确认，ID: {}", id);
        
        // 2. 获取data_code字段（逗号分隔的字符串）
        String dataCodeStr = governanceEntity.getDataCode();
        if (dataCodeStr == null || dataCodeStr.trim().isEmpty()) {
            logger.error("data_code字段为空，ID: {}", id);
            throw new RuntimeException("data_code字段为空，ID: " + id);
        }
        logger.info("获取到data_code字符串: {}", dataCodeStr);
        
        // 3. 解析data_code字符串为数组
        List<String> dataCodes = Arrays.stream(dataCodeStr.split(","))
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .collect(Collectors.toList());
        logger.info("解析出的数据代码列表: {}", dataCodes);
        
        if (dataCodes.isEmpty()) {
            logger.error("解析后的数据代码列表为空，ID: {}", id);
            throw new RuntimeException("解析后的数据代码列表为空，ID: " + id);
        }
        
        // 4. 根据dataCodes从external_data_info表中获取file_path
        List<String> filePaths = fileMapper.getFilePathsByIds(dataCodes);
        if (filePaths == null || filePaths.isEmpty()) {
            logger.error("未找到任何文件路径，数据代码: {}", dataCodes);
            throw new DataNotFoundException("未找到对应的文件路径");
        }
        logger.info("获取到文件路径数量: {}, 路径: {}", filePaths.size(), filePaths);
        
        // 5. 构造工作流启动参数 {"args": [file_path1, file_path2, ...]}
        String workflowInitParams = buildWorkflowArgs(filePaths);
        logger.info("构造的工作流参数: {}", workflowInitParams);
        
        // 6. 启动工作流
        Long instanceId = workflowService.startWorkflow(workflowInitParams);
        if (instanceId == null) {
            logger.error("工作流启动失败");
            throw new RuntimeException("工作流启动失败");
        }
        logger.info("工作流启动成功，实例ID: {}", instanceId);
        
        // 7. 更新data_governance_info表中对应记录的task_id和task_receive_time字段
        LocalDateTime taskReceiveTime = LocalDateTime.now();
        int updatedCount = dataGovernanceMapper.updateTaskIdByDataCodes(dataCodes, String.valueOf(instanceId), taskReceiveTime);
        if (updatedCount != dataCodes.size()) {
            logger.warn("部分记录的task_id和task_receive_time更新失败，期望: {}, 实际: {}", dataCodes.size(), updatedCount);
        } else {
            logger.info("✔️  所有记录的task_id和task_receive_time更新成功，任务接收时间: {}", taskReceiveTime);
        }
        
        // 8. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("instanceId", instanceId);
        result.put("governanceId", id);
        result.put("dataCodes", dataCodes);
        result.put("filePaths", filePaths);
        result.put("updatedCount", updatedCount);
        result.put("taskReceiveTime", taskReceiveTime);
        
        logger.info("✔️  治理任务启动完成，实例ID: {}, 治理记录ID: {}, 处理数据代码数量: {}", instanceId, id, dataCodes.size());
        return result;
    }

    /**
     * 构造工作流启动参数
     * @param filePaths 文件路径列表
     * @return JSON格式的参数字符串
     */
    private String buildWorkflowArgs(List<String> filePaths) {
        try {
            ObjectNode argsJson = objectMapper.createObjectNode();
            ArrayNode argsArray = objectMapper.createArrayNode();
            
            for (String filePath : filePaths) {
                if (filePath != null && !filePath.trim().isEmpty()) {
                    // 处理Windows路径：将单反斜杠替换为双反斜杠以适配PowerJob要求
                    String processedPath = processWindowsPath(filePath);
                    argsArray.add(processedPath);
                    logger.debug("原始路径: {} -> 处理后路径: {}", filePath, processedPath);
                }
            }
            
            argsJson.set("args", argsArray);
            String result = objectMapper.writeValueAsString(argsJson);
            logger.debug("构造的工作流参数: {}", result);
            return result;
        } catch (Exception e) {
            logger.error("构造工作流参数异常: ", e);
            return "{\"args\": []}";
        }
    }

    /**
     * 处理Windows路径，确保反斜杠正确转义以适配PowerJob
     * @param filePath 原始文件路径
     * @return 处理后的文件路径
     */
    private String processWindowsPath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return filePath;
        }
        
        // 对于Windows路径，确保反斜杠被正确处理
        // 将单个反斜杠替换为双反斜杠（如果还没有转义的话）
        String processedPath = filePath;
        
        // 检查是否是Windows路径（包含反斜杠或盘符）
        if (filePath.contains("\\") || filePath.matches("^[A-Za-z]:.*")) {
            // 先将已经转义的双反斜杠临时替换，避免重复转义
            processedPath = filePath.replace("\\\\", "__TEMP_DOUBLE_SLASH__");
            // 将单反斜杠替换为双反斜杠
            processedPath = processedPath.replace("\\", "\\\\");
            // 恢复原来就是双反斜杠的部分
            processedPath = processedPath.replace("__TEMP_DOUBLE_SLASH__", "\\\\");
        }
        
        return processedPath;
    }

    @Override
    public SyncStatusResult syncGovernanceStatus() {
        logger.info("==================== 开始同步治理任务状态 ====================");
        
        SyncStatusResult result = new SyncStatusResult();
        
        // 1. 查询所有治理中的记录
        List<DataGovernanceEntity> governanceList = dataGovernanceMapper.findAllInGovernance();
        result.setTotalCount(governanceList.size());
        logger.info("查询到 {} 条治理中的记录", governanceList.size());
        
        if (governanceList.isEmpty()) {
            logger.info("没有需要同步的治理记录");
            logger.info("==================== 同步完成 ====================");
            return result;
        }
        
        int updatedCount = 0;
        int skippedCount = 0;
        int failedCount = 0;
        
        // 2. 遍历每条记录，查询工作流实例状态并更新时间
        for (DataGovernanceEntity entity : governanceList) {
            String dataCode = entity.getDataCode();
            String taskId = entity.getTaskId();
            
            logger.info("处理第 {} 条记录，data_code: {}, task_id: {}", 
                       governanceList.indexOf(entity) + 1, dataCode, taskId);
            
            try {
                // 3. 调用9030端口API查询工作流实例信息
                WorkflowInstanceResponse workflowInstance = queryWorkflowInstance(taskId);
                
                if (workflowInstance == null) {
                    logger.warn("❌ 未找到工作流实例，task_id: {}", taskId);
                    failedCount++;
                    continue;
                }
                
                logger.info("工作流实例信息 - actualTriggerTime: {}, finishedTime: {}, status: {}", 
                           workflowInstance.getActualTriggerTime(), 
                           workflowInstance.getFinishedTime(),
                           workflowInstance.getStatus());
                
                // 4. 解析时间并更新到数据库
                LocalDateTime governanceStartTime = parseDateTime(workflowInstance.getActualTriggerTime());
                LocalDateTime governanceEndTime = parseDateTime(workflowInstance.getFinishedTime());
                
                if (governanceStartTime != null || governanceEndTime != null) {
                    int updated = dataGovernanceMapper.updateGovernanceTimesByDataCode(
                        dataCode, governanceStartTime, governanceEndTime);
                    
                    if (updated > 0) {
                        updatedCount++;
                        logger.info("✔️  成功更新治理时间，data_code: {}", dataCode);
                    } else {
                        logger.error("❌ 更新治理时间失败，data_code: {}", dataCode);
                        failedCount++;
                    }
                } else {
                    logger.warn("⏳ 时间信息无效，跳过更新，data_code: {}", dataCode);
                    skippedCount++;
                }
                
            } catch (Exception e) {
                logger.error("❌ 处理记录时发生异常，data_code: {}, task_id: {}", dataCode, taskId, e);
                failedCount++;
            }
        }
        
        result.setUpdatedCount(updatedCount);
        result.setSkippedCount(skippedCount);
        result.setFailedCount(failedCount);
        
        logger.info("======================================================");
        logger.info("治理任务状态同步完成!");
        logger.info("总记录数: {}", result.getTotalCount());
        logger.info("成功更新: {} 条", updatedCount);
        logger.info("跳过记录: {} 条", skippedCount);
        logger.info("失败记录: {} 条", failedCount);
        logger.info("==================== 同步结束 ====================");
        
        return result;
    }
    
    /**
     * 查询工作流实例信息
     * @param instanceId 实例ID
     * @return 工作流实例响应
     */
    private WorkflowInstanceResponse queryWorkflowInstance(String instanceId) {
        try {
            String url = schedulerConfig.getBaseUrl() + "/api/workflow/instance/" + instanceId;
            logger.info("查询工作流实例，URL: {}", url);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, null, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode rootNode = objectMapper.readTree(response.getBody());
                JsonNode dataNode = rootNode.get("data");
                
                if (dataNode != null && !dataNode.isNull()) {
                    return objectMapper.treeToValue(dataNode, WorkflowInstanceResponse.class);
                } else {
                    logger.warn("响应中未找到data字段");
                }
            } else {
                logger.error("API调用失败，状态码: {}", response.getStatusCode());
            }
            
        } catch (Exception e) {
            logger.error("查询工作流实例异常，instanceId: {}", instanceId, e);
        }
        
        return null;
    }
    
    /**
     * 解析日期时间字符串
     * @param dateTimeString 日期时间字符串，格式：yyyy-MM-dd HH:mm:ss
     * @return LocalDateTime对象，解析失败返回null
     */
    private LocalDateTime parseDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            return null;
        }
        
        try {
            return LocalDateTime.parse(dateTimeString.trim(), dateTimeFormatter);
        } catch (Exception e) {
            logger.error("解析日期时间失败: {}", dateTimeString, e);
            return null;
        }
    }
}

