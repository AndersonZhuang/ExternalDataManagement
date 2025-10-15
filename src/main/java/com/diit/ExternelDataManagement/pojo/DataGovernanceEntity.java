package com.diit.ExternelDataManagement.pojo;

import java.time.LocalDateTime;

public class DataGovernanceEntity {

    private String id;
    private String receiveCode;
    private LocalDateTime taskReceiveTime;
    private String governanceStatus;
    private String dataCode;
    private String taskId;
    private LocalDateTime governanceStartTime;
    private LocalDateTime governanceEndTime;

    public DataGovernanceEntity() {
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

    public LocalDateTime getTaskReceiveTime() {
        return taskReceiveTime;
    }

    public void setTaskReceiveTime(LocalDateTime taskReceiveTime) {
        this.taskReceiveTime = taskReceiveTime;
    }

    public String getGovernanceStatus() {
        return governanceStatus;
    }

    public void setGovernanceStatus(String governanceStatus) {
        this.governanceStatus = governanceStatus;
    }

    public String getDataCode() {
        return dataCode;
    }

    public void setDataCode(String dataCode) {
        this.dataCode = dataCode;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public LocalDateTime getGovernanceStartTime() {
        return governanceStartTime;
    }

    public void setGovernanceStartTime(LocalDateTime governanceStartTime) {
        this.governanceStartTime = governanceStartTime;
    }

    public LocalDateTime getGovernanceEndTime() {
        return governanceEndTime;
    }

    public void setGovernanceEndTime(LocalDateTime governanceEndTime) {
        this.governanceEndTime = governanceEndTime;
    }

    @Override
    public String toString() {
        return "DataGovernanceEntity{" +
                "id='" + id + '\'' +
                ", receiveCode='" + receiveCode + '\'' +
                ", taskReceiveTime=" + taskReceiveTime +
                ", governanceStatus='" + governanceStatus + '\'' +
                ", dataCode='" + dataCode + '\'' +
                ", taskId='" + taskId + '\'' +
                ", governanceStartTime=" + governanceStartTime +
                ", governanceEndTime=" + governanceEndTime +
                '}';
    }
}
