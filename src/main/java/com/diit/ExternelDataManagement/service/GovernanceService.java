package com.diit.ExternelDataManagement.service;

import com.diit.ExternelDataManagement.pojo.SyncStatusResult;

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
}

