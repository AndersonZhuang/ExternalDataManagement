package com.diit.ExternelDataManagement.pojo;

import java.util.ArrayList;
import java.util.List;

public class SyncStatusResult {

    private int totalCount;              // 总共查询到的质检中记录数
    private int updatedCount;            // 成功更新的记录数
    private int skippedCount;            // 跳过的记录数（仍在运行中）
    private int failedCount;             // 失败的记录数
    private List<SyncDetail> details;    // 详细信息列表

    public SyncStatusResult() {
        this.details = new ArrayList<>();
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getUpdatedCount() {
        return updatedCount;
    }

    public void setUpdatedCount(int updatedCount) {
        this.updatedCount = updatedCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(int skippedCount) {
        this.skippedCount = skippedCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public List<SyncDetail> getDetails() {
        return details;
    }

    public void setDetails(List<SyncDetail> details) {
        this.details = details;
    }

    public void addDetail(SyncDetail detail) {
        this.details.add(detail);
    }

    public static class SyncDetail {
        private String id;                  // 数据记录ID
        private String receiveCode;         // 接收编码
        private String instanceId;          // 工作流实例ID
        private Integer workflowStatus;     // 工作流状态
        private String workflowStatusDesc;  // 工作流状态描述
        private String oldQualityStatus;    // 原质检状态
        private String newQualityStatus;    // 新质检状态
        private String action;              // 操作：updated/skipped/failed
        private String message;             // 消息说明

        public SyncDetail() {
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getReceiveCode() {
            return receiveCode;
        }

        public void setReceiveCode(String receiveCode) {
            this.receiveCode = receiveCode;
        }

        public String getInstanceId() {
            return instanceId;
        }

        public void setInstanceId(String instanceId) {
            this.instanceId = instanceId;
        }

        public Integer getWorkflowStatus() {
            return workflowStatus;
        }

        public void setWorkflowStatus(Integer workflowStatus) {
            this.workflowStatus = workflowStatus;
        }

        public String getWorkflowStatusDesc() {
            return workflowStatusDesc;
        }

        public void setWorkflowStatusDesc(String workflowStatusDesc) {
            this.workflowStatusDesc = workflowStatusDesc;
        }

        public String getOldQualityStatus() {
            return oldQualityStatus;
        }

        public void setOldQualityStatus(String oldQualityStatus) {
            this.oldQualityStatus = oldQualityStatus;
        }

        public String getNewQualityStatus() {
            return newQualityStatus;
        }

        public void setNewQualityStatus(String newQualityStatus) {
            this.newQualityStatus = newQualityStatus;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return "SyncDetail{" +
                    "id='" + id + '\'' +
                    ", receiveCode='" + receiveCode + '\'' +
                    ", instanceId='" + instanceId + '\'' +
                    ", workflowStatus=" + workflowStatus +
                    ", workflowStatusDesc='" + workflowStatusDesc + '\'' +
                    ", oldQualityStatus='" + oldQualityStatus + '\'' +
                    ", newQualityStatus='" + newQualityStatus + '\'' +
                    ", action='" + action + '\'' +
                    ", message='" + message + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "SyncStatusResult{" +
                "totalCount=" + totalCount +
                ", updatedCount=" + updatedCount +
                ", skippedCount=" + skippedCount +
                ", failedCount=" + failedCount +
                ", details=" + details +
                '}';
    }
}
