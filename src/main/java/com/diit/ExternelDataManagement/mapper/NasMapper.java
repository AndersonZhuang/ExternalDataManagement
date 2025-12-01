package com.diit.ExternelDataManagement.mapper;

import com.diit.ExternelDataManagement.pojo.NasEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * NAS数据访问接口
 * 
 * @author Assistant
 * @since 2025-01-XX
 */
@Mapper
public interface NasMapper {

    /**
     * 插入NAS记录
     */
    @Insert("INSERT INTO nas_info (ID, NAS_NAME, NAS_IP) VALUES (#{id}, #{nasName}, #{nasIp}::jsonb)")
    int insert(NasEntity nasEntity);

    /**
     * 根据ID删除NAS记录
     */
    @Delete("DELETE FROM nas_info WHERE ID = #{id}")
    int deleteById(@Param("id") String id);

    /**
     * 更新NAS记录
     */
    @Update("UPDATE nas_info SET NAS_NAME = #{nasName}, NAS_IP = #{nasIp}::jsonb WHERE ID = #{id}")
    int update(NasEntity nasEntity);

    /**
     * 根据ID查询NAS记录
     */
    @Select("SELECT * FROM nas_info WHERE ID = #{id}")
    @Results({
        @Result(property = "id", column = "ID"),
        @Result(property = "nasName", column = "NAS_NAME"),
        @Result(property = "nasIp", column = "NAS_IP")
    })
    NasEntity findById(@Param("id") String id);

    /**
     * 查询所有NAS记录
     */
    @Select("SELECT * FROM nas_info ORDER BY ID")
    @Results({
        @Result(property = "id", column = "ID"),
        @Result(property = "nasName", column = "NAS_NAME"),
        @Result(property = "nasIp", column = "NAS_IP")
    })
    List<NasEntity> findAll();

    /**
     * 根据NAS名称查询
     */
    @Select("SELECT * FROM nas_info WHERE NAS_NAME = #{nasName}")
    @Results({
        @Result(property = "id", column = "ID"),
        @Result(property = "nasName", column = "NAS_NAME"),
        @Result(property = "nasIp", column = "NAS_IP")
    })
    List<NasEntity> findByNasName(@Param("nasName") String nasName);

    /**
     * 根据NAS地址查询（JSON格式匹配）
     */
    @Select("SELECT * FROM nas_info WHERE NAS_IP::text = #{nasIp}::text")
    @Results({
        @Result(property = "id", column = "ID"),
        @Result(property = "nasName", column = "NAS_NAME"),
        @Result(property = "nasIp", column = "NAS_IP")
    })
    List<NasEntity> findByNasIp(@Param("nasIp") String nasIp);
}

