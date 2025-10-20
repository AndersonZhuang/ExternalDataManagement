package com.diit.ExternelDataManagement.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkflowInstanceResponse {

    @JsonProperty("actualTriggerTime")
    private String actualTriggerTime;

    @JsonProperty("finishedTime")
    private String finishedTime;

    @JsonProperty("status")
    private Integer status;

    public WorkflowInstanceResponse() {
    }

    public String getActualTriggerTime() {
        return actualTriggerTime;
    }

    public void setActualTriggerTime(String actualTriggerTime) {
        this.actualTriggerTime = actualTriggerTime;
    }

    public String getFinishedTime() {
        return finishedTime;
    }

    public void setFinishedTime(String finishedTime) {
        this.finishedTime = finishedTime;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "WorkflowInstanceResponse{" +
                "actualTriggerTime='" + actualTriggerTime + '\'' +
                ", finishedTime='" + finishedTime + '\'' +
                ", status=" + status +
                '}';
    }
}
