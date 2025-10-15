package com.diit.ExternelDataManagement.mapper2.workflow;

import com.diit.ExternelDataManagement.pojo.WorkflowInstanceEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WorkflowInstanceMapper {

    @Select("SELECT wf_instance_id, status FROM workflow_instance_info WHERE wf_instance_id = #{instanceId}")
    @Results({
        @Result(property = "id", column = "wf_instance_id"),
        @Result(property = "status", column = "status")
    })
    WorkflowInstanceEntity findById(@Param("instanceId") Long instanceId);
}
