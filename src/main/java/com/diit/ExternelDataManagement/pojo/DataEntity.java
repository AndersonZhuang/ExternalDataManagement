package com.diit.ExternelDataManagement.pojo;

import java.time.LocalDateTime;

public class DataEntity {

    private String id;
    private String receiveCode;
    private LocalDateTime receiveTime;
    private String qualityStatus;
    private String receiveStatus;
    private String instanceId;

    public DataEntity() {
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

    public LocalDateTime getReceiveTime() {
        return receiveTime;
    }

    public void setReceiveTime(LocalDateTime receiveTime) {
        this.receiveTime = receiveTime;
    }

    public String getQualityStatus() {
        return qualityStatus;
    }

    public void setQualityStatus(String qualityStatus) {
        this.qualityStatus = qualityStatus;
    }

    public String getReceiveStatus() {
        return receiveStatus;
    }

    public void setReceiveStatus(String receiveStatus) {
        this.receiveStatus = receiveStatus;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    @Override
    public String toString() {
        return "DataEntity{" +
                "id=" + id +
                ", receiveCode='" + receiveCode + '\'' +
                ", receiveTime=" + receiveTime +
                ", qualityStatus='" + qualityStatus + '\'' +
                ", receiveStatus='" + receiveStatus + '\'' +
                ", instanceId=" + instanceId +
                '}';
    }
}