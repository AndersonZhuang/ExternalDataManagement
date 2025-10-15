package com.diit.ExternelDataManagement.controller;

import com.diit.ExternelDataManagement.common.APIResponse;
import com.diit.ExternelDataManagement.pojo.SyncStatusResult;
import com.diit.ExternelDataManagement.service.GovernanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/governance")
@Tag(name = "治理管理", description = "数据治理任务管理接口")
public class GovernanceController {

    @Autowired
    private GovernanceService governanceService;

    @PostMapping("/start/{workflowId}")
    @Operation(summary = "启动治理任务", 
               description = "根据数据代码列表和工作流ID启动治理任务。" +
                           "系统会根据数据代码查询external_data_info表获取文件路径，" +
                           "构造工作流参数并启动工作流，" +
                           "然后将工作流实例ID更新到data_governance_info表的task_id字段，" +
                           "同时记录task_receive_time时间戳。",
               tags = "治理管理")
    @RequestBody(description = "数据代码列表", 
                required = true,
                content = @Content(mediaType = "application/json",
                                 schema = @Schema(type = "array"),
                                 examples = @ExampleObject(name = "示例1", 
                                                         value = "[\n" +
                                                                "  \"DATA001\",\n" +
                                                                "  \"DATA002\",\n" +
                                                                "  \"DATA003\"\n" +
                                                                "]")))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "启动成功",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(type = "object"),
                                     examples = @ExampleObject(name = "成功响应", 
                                                             value = "{\n" +
                                                                     "  \"code\": 200,\n" +
                                                                     "  \"message\": \"success\",\n" +
                                                                     "  \"data\": {\n" +
                                                                     "    \"instanceId\": 789456,\n" +
                                                                     "    \"dataCodes\": [\"DATA001\", \"DATA002\", \"DATA003\"],\n" +
                                                                     "    \"filePaths\": [\"C:\\\\Users\\\\test\\\\file1.shp\", \"C:\\\\Users\\\\test\\\\file2.shp\"],\n" +
                                                                     "    \"updatedCount\": 3,\n" +
                                                                     "    \"taskReceiveTime\": \"2024-10-14T15:30:45.123\"\n" +
                                                                     "  }\n" +
                                                                     "}"))),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public APIResponse<Object> startGovernanceTask(
            @Parameter(description = "工作流ID，用于指定启动的工作流类型", required = true, example = "123456")
            @PathVariable int workflowId,
            @org.springframework.web.bind.annotation.RequestBody List<String> ids) {
        Object result = governanceService.startGovernanceTask(ids, workflowId);
        return APIResponse.ok(result);
    }

    @PostMapping("/sync-status")
    @Operation(summary = "同步治理任务状态", 
               description = "查询所有治理中的记录，调用工作流实例查询API获取任务执行时间，" +
                           "将actualTriggerTime和finishedTime同步到data_governance_info表的" +
                           "governance_start_time和governance_end_time字段，返回详细的同步结果。")
    public APIResponse<SyncStatusResult> syncGovernanceStatus() {
        SyncStatusResult result = governanceService.syncGovernanceStatus();
        return APIResponse.ok(result);
    }
}

