package com.diit.ExternelDataManagement.mapper;

import com.diit.ExternelDataManagement.dto.CodeFilePathMappingDTO;
import com.diit.ExternelDataManagement.pojo.FileEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface FileMapper {

    @Insert("INSERT INTO external_data_info (ID, RECEIVE_CODE, FILE_PATH, DATA_TYPE, FILE_TYPE, BBOX, DATA_SIZE, " +
            "TOTAL_OBJECT_NUM, TOTAL_AREA, LAYER_NAME) " +
            "VALUES (#{id}, #{receiveCode}, #{filePath}, #{dataType}, #{fileType}, #{bbox}, #{dataSize}, " +
            "#{totalObjectNum}, #{totalArea}, #{layerName})")
    int insert(FileEntity fileEntity);

    @Select("SELECT FILE_PATH FROM receive_external_package_info WHERE RECEIVE_CODE = #{receiveCode}")
    String getFilePathByReceiveCode(@Param("receiveCode") String receiveCode);

    @Select("SELECT * FROM external_data_info WHERE RECEIVE_CODE = #{receiveCode}")
    @Results({
        @Result(property = "id", column = "ID"),
        @Result(property = "receiveCode", column = "RECEIVE_CODE"),
        @Result(property = "filePath", column = "FILE_PATH"),
        @Result(property = "dataType", column = "DATA_TYPE"),
        @Result(property = "fileType", column = "FILE_TYPE"),
        @Result(property = "bbox", column = "BBOX"),
        @Result(property = "dataSize", column = "DATA_SIZE"),
        @Result(property = "totalObjectNum", column = "TOTAL_OBJECT_NUM"),
        @Result(property = "totalArea", column = "TOTAL_AREA"),
        @Result(property = "layerName", column = "LAYER_NAME")
    })
    List<FileEntity> findByReceiveCode(@Param("receiveCode") String receiveCode);

    @Select("SELECT FILE_PATH FROM external_data_info WHERE ID = #{id}")
    String getFilePathById(@Param("id") String id);

    @Select("<script>" +
            "SELECT FILE_PATH FROM external_data_info WHERE ID IN " +
            "<foreach item='id' collection='ids' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<String> getFilePathsByIds(@Param("ids") List<String> ids);

    /**
     * 批量查询code和filepath的映射关系
     * @param codes 数据代码列表
     * @return code和filepath映射关系列表
     */
    @Select("<script>" +
            "SELECT ID as code, FILE_PATH as filePath FROM external_data_info WHERE ID IN " +
            "<foreach item='code' collection='codes' open='(' separator=',' close=')'>" +
            "#{code}" +
            "</foreach>" +
            "</script>")
    @Results({
        @Result(property = "code", column = "code"),
        @Result(property = "filePath", column = "filePath")
    })
    List<CodeFilePathMappingDTO> getCodeFilePathMappings(@Param("codes") List<String> codes);
}