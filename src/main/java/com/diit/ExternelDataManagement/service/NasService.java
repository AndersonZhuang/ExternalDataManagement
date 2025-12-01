package com.diit.ExternelDataManagement.service;

import com.diit.ExternelDataManagement.pojo.NasEntity;

import java.util.List;

/**
 * NAS服务接口
 * 
 * @author Assistant
 * @since 2025-01-XX
 */
public interface NasService {

    /**
     * 创建NAS记录
     */
    NasEntity createNas(NasEntity nasEntity);

    /**
     * 根据ID删除NAS记录
     */
    boolean deleteNas(String id);

    /**
     * 更新NAS记录
     */
    NasEntity updateNas(NasEntity nasEntity);

    /**
     * 根据ID查询NAS记录
     */
    NasEntity getNasById(String id);

    /**
     * 查询所有NAS记录
     */
    List<NasEntity> getAllNas();
}

