package com.diit.ExternelDataManagement.mapper;

import com.diit.ExternelDataManagement.pojo.DataEntity;
import org.apache.ibatis.annotations.*;

@Mapper
public interface DataMapper {

    @Select("SELECT id, receive_code, receive_time, quality_status, receive_status, instance_id FROM receive_external_package_info WHERE id = #{id}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "receiveCode", column = "receive_code"),
        @Result(property = "receiveTime", column = "receive_time"),
        @Result(property = "qualityStatus", column = "quality_status"),
        @Result(property = "receiveStatus", column = "receive_status"),
        @Result(property = "instanceId", column = "instance_id")
    })
    DataEntity findById(@Param("id") String id);

    @Update("UPDATE receive_external_package_info SET receive_code = #{receiveCode}, receive_time = #{receiveTime}, " +
            "quality_status = #{qualityStatus}, receive_status = #{receiveStatus}, instance_id = #{instanceId} WHERE id = #{id}")
    int updateStatusFields(DataEntity dataEntity);

    @Update("UPDATE receive_external_package_info SET quality_status = #{qualityStatus}, receive_status = #{receiveStatus} WHERE receive_code = #{receiveCode}")
    int updateStatusByReceiveCode(@Param("receiveCode") String receiveCode,
                                  @Param("qualityStatus") String qualityStatus,
                                  @Param("receiveStatus") String receiveStatus);

    @Select("SELECT COUNT(*) FROM receive_external_package_info WHERE id = #{id}")
    int existsById(@Param("id") String id);

    @Select("SELECT COUNT(*) FROM receive_external_package_info WHERE receive_code = #{receiveCode}")
    int existsByReceiveCode(@Param("receiveCode") String receiveCode);
}