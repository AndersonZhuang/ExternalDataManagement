package com.diit.ExternelDataManagement.service;

import com.diit.ExternelDataManagement.pojo.SyncStatusResult;

import java.util.List;

public interface GovernanceService {

    /**
     * 启动治理任务
     * @param ids 数据代码列表（data_code）
     * @param workflowId 工作流ID
     * @return 治理任务结果
     */
    Object startGovernanceTask(List<String> ids, int workflowId);

    /**
     * 同步治理任务状态
     * @return 同步结果统计
     */
    SyncStatusResult syncGovernanceStatus();
}

