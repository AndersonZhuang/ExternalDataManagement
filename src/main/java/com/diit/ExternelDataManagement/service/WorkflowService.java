package com.diit.ExternelDataManagement.service;

public interface WorkflowService {

    Long startWorkflow();
    
    Long startWorkflow(String initParams);
    
    Long startWorkflow(String initParams, String workflowId);
}