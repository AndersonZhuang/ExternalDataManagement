package com.diit.ExternelDataManagement.mapper;

import com.diit.ExternelDataManagement.pojo.FileEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface FileMapper {

    @Insert("INSERT INTO external_data_info (id, receive_code, file_path, data_type, bbox, data_size, " +
            "total_object_num, total_area, layer_name) " +
            "VALUES (#{id}, #{receiveCode}, #{filePath}, #{dataType}, #{bbox}, #{dataSize}, " +
            "#{totalObjectNum}, #{totalArea}, #{layerName})")
    int insert(FileEntity fileEntity);

    @Select("SELECT file_path FROM receive_external_package_info WHERE receive_code = #{receiveCode}")
    String getFilePathByReceiveCode(@Param("receiveCode") String receiveCode);

    @Select("SELECT * FROM external_data_info WHERE receive_code = #{receiveCode}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "receiveCode", column = "receive_code"),
        @Result(property = "filePath", column = "file_path"),
        @Result(property = "dataType", column = "data_type"),
        @Result(property = "bbox", column = "bbox"),
        @Result(property = "dataSize", column = "data_size"),
        @Result(property = "totalObjectNum", column = "total_object_num"),
        @Result(property = "totalArea", column = "total_area"),
        @Result(property = "layerName", column = "layer_name")
    })
    List<FileEntity> findByReceiveCode(@Param("receiveCode") String receiveCode);

    @Select("SELECT file_path FROM external_data_info WHERE id = #{id}")
    String getFilePathById(@Param("id") String id);

    @Select("<script>" +
            "SELECT file_path FROM external_data_info WHERE id IN " +
            "<foreach item='id' collection='ids' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<String> getFilePathsByIds(@Param("ids") List<String> ids);
}