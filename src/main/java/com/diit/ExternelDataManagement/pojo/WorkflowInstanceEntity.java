package com.diit.ExternelDataManagement.pojo;

public class WorkflowInstanceEntity {

    private Long id;
    private Integer status;

    public WorkflowInstanceEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "WorkflowInstanceEntity{" +
                "id=" + id +
                ", status=" + status +
                '}';
    }
}
