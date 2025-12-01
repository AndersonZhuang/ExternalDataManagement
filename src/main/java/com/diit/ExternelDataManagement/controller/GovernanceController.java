package com.diit.ExternelDataManagement.controller;

import com.diit.ExternelDataManagement.common.APIResponse;
import com.diit.ExternelDataManagement.dto.CodeFilePathMappingDTO;
import com.diit.ExternelDataManagement.pojo.SyncStatusResult;
import com.diit.ExternelDataManagement.service.GovernanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/governance")
@Tag(name = "治理管理", description = "数据治理任务管理接口")
public class GovernanceController {

    @Autowired
    private GovernanceService governanceService;

    @PostMapping("/start/{workflowId}/{id}")
    @Operation(summary = "启动治理任务", 
               description = "根据治理信息记录ID和工作流ID启动治理任务。" +
                           "系统会根据治理记录ID查询data_governance_info表获取data_code字段，" +
                           "解析逗号分隔的data_code为数组，然后查询external_data_info表获取文件路径，" +
                           "构造工作流参数并启动工作流，最后更新task_id和task_receive_time字段。")
    public APIResponse<Object> startGovernanceTask(
            @Parameter(description = "工作流ID，用于指定启动的工作流类型", required = true, example = "123456")
            @PathVariable int workflowId,
            @Parameter(description = "治理信息记录ID，对应data_governance_info表的id字段", required = true, example = "GOV_001")
            @PathVariable String id) {
        Object result = governanceService.startGovernanceTask(id, workflowId);
        return APIResponse.ok(result);
    }

    @PostMapping("/sync-status")
    @Operation(summary = "同步治理任务状态", description = "同步所有治理中记录的状态信息")
    public APIResponse<SyncStatusResult> syncGovernanceStatus() {
        SyncStatusResult result = governanceService.syncGovernanceStatus();
        return APIResponse.ok(result);
    }

    @PostMapping("/code-filepath-mappings")
    @Operation(summary = "批量查询code和filepath映射", 
               description = "根据传入的code列表，批量查询对应的文件路径，返回code和filepath的映射关系列表。" +
                           "适用于治理提交时需要根据数据源代码获取文件路径的场景。")
    public APIResponse<List<CodeFilePathMappingDTO>> getCodeFilePathMappings(
            @Parameter(description = "数据代码列表，支持批量查询", required = true)
            @RequestBody List<String> codes) {
        List<CodeFilePathMappingDTO> mappings = governanceService.getCodeFilePathMappings(codes);
        return APIResponse.ok(mappings);
    }
}

