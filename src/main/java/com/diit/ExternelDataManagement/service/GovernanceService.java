package com.diit.ExternelDataManagement.service;

import com.diit.ExternelDataManagement.dto.CodeFilePathMappingDTO;
import com.diit.ExternelDataManagement.pojo.SyncStatusResult;

import java.util.List;

public interface GovernanceService {

    /**
     * 启动治理任务
     * @param id 治理信息记录ID（data_governance_info表的id字段）
     * @param workflowId 工作流ID
     * @return 治理任务结果
     */
    Object startGovernanceTask(String id, int workflowId);

    /**
     * 同步治理任务状态
     * @return 同步结果统计
     */
    SyncStatusResult syncGovernanceStatus();

    /**
     * 批量查询code和filepath的映射关系
     * @param codes 数据代码列表
     * @return code和filepath映射关系列表
     */
    List<CodeFilePathMappingDTO> getCodeFilePathMappings(List<String> codes);
}

