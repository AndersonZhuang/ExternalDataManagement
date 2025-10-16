package com.diit.ExternelDataManagement.mapper;

import com.diit.ExternelDataManagement.pojo.DataGovernanceEntity;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DataGovernanceMapper {

    @Select("SELECT id, receive_code, task_receive_time, governance_status, data_code, task_id, governance_start_time, governance_end_time FROM data_governance_info WHERE id = #{id}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "receiveCode", column = "receive_code"),
        @Result(property = "taskReceiveTime", column = "task_receive_time"),
        @Result(property = "governanceStatus", column = "governance_status"),
        @Result(property = "dataCode", column = "data_code"),
        @Result(property = "taskId", column = "task_id"),
        @Result(property = "governanceStartTime", column = "governance_start_time"),
        @Result(property = "governanceEndTime", column = "governance_end_time")
    })
    DataGovernanceEntity findById(@Param("id") String id);

    @Select("SELECT data_code FROM data_governance_info WHERE id = #{id}")
    String getDataCodeById(@Param("id") String id);

    @Update("UPDATE data_governance_info SET receive_code = #{receiveCode}, task_receive_time = #{taskReceiveTime}, governance_status = #{governanceStatus} WHERE id = #{id}")
    int updateGovernanceInfo(@Param("id") String id,
                           @Param("receiveCode") String receiveCode,
                           @Param("taskReceiveTime") LocalDateTime taskReceiveTime,
                           @Param("governanceStatus") String governanceStatus);

    @Update("UPDATE data_governance_info SET task_id = #{taskId}, task_receive_time = #{taskReceiveTime} WHERE data_code = #{dataCode}")
    int updateTaskIdByDataCode(@Param("dataCode") String dataCode, 
                              @Param("taskId") String taskId, 
                              @Param("taskReceiveTime") LocalDateTime taskReceiveTime);

    @Update("<script>" +
            "UPDATE data_governance_info SET task_id = #{taskId}, task_receive_time = #{taskReceiveTime} WHERE data_code IN " +
            "<foreach item='dataCode' collection='dataCodes' open='(' separator=',' close=')'>" +
            "#{dataCode}" +
            "</foreach>" +
            "</script>")
    int updateTaskIdByDataCodes(@Param("dataCodes") List<String> dataCodes, 
                               @Param("taskId") String taskId, 
                               @Param("taskReceiveTime") LocalDateTime taskReceiveTime);

    @Select("SELECT COUNT(*) FROM data_governance_info WHERE id = #{id}")
    int existsById(@Param("id") String id);

    @Select("SELECT COUNT(*) FROM data_governance_info WHERE data_code = #{dataCode}")
    int existsByDataCode(@Param("dataCode") String dataCode);

    @Select("<script>" +
            "SELECT COUNT(*) FROM data_governance_info WHERE data_code IN " +
            "<foreach item='dataCode' collection='dataCodes' open='(' separator=',' close=')'>" +
            "#{dataCode}" +
            "</foreach>" +
            "</script>")
    int countByDataCodes(@Param("dataCodes") List<String> dataCodes);

    @Select("SELECT data_code, task_id, governance_status FROM data_governance_info WHERE task_id IS NOT NULL AND task_id != '' AND governance_status = '治理中'")
    @Results({
        @Result(property = "dataCode", column = "data_code"),
        @Result(property = "taskId", column = "task_id"),
        @Result(property = "governanceStatus", column = "governance_status")
    })
    List<DataGovernanceEntity> findAllInGovernance();

    @Update("UPDATE data_governance_info SET governance_start_time = #{governanceStartTime}, governance_end_time = #{governanceEndTime} WHERE data_code = #{dataCode}")
    int updateGovernanceTimesByDataCode(@Param("dataCode") String dataCode,
                                       @Param("governanceStartTime") LocalDateTime governanceStartTime,
                                       @Param("governanceEndTime") LocalDateTime governanceEndTime);
}
