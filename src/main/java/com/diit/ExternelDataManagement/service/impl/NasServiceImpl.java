package com.diit.ExternelDataManagement.service.impl;

import com.diit.ExternelDataManagement.exception.DataNotFoundException;
import com.diit.ExternelDataManagement.mapper.NasMapper;
import com.diit.ExternelDataManagement.pojo.NasEntity;
import com.diit.ExternelDataManagement.service.NasService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * NAS服务实现类
 * 
 * @author Assistant
 * @since 2025-01-XX
 */
@Service
public class NasServiceImpl implements NasService {

    private static final Logger logger = LoggerFactory.getLogger(NasServiceImpl.class);

    @Autowired
    private NasMapper nasMapper;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public NasEntity createNas(NasEntity nasEntity) {
        logger.info("创建NAS记录: {}", nasEntity);

        // 生成唯一ID
        if (nasEntity.getId() == null || nasEntity.getId().trim().isEmpty()) {
            nasEntity.setId(generateUniqueId());
        }

        // 验证必填字段
        if (nasEntity.getNasName() == null || nasEntity.getNasName().trim().isEmpty()) {
            throw new IllegalArgumentException("NAS名称不能为空");
        }
        if (nasEntity.getNasIp() == null || nasEntity.getNasIp().trim().isEmpty()) {
            throw new IllegalArgumentException("NAS地址（JSON格式）不能为空");
        }
        
        // 验证JSON格式
        validateJsonFormat(nasEntity.getNasIp(), "NAS地址");

        // 检查NAS名称是否已存在
        List<NasEntity> existingByName = nasMapper.findByNasName(nasEntity.getNasName());
        if (!existingByName.isEmpty()) {
            throw new IllegalArgumentException("NAS名称已存在: " + nasEntity.getNasName());
        }

        // 插入数据库
        int result = nasMapper.insert(nasEntity);
        if (result > 0) {
            logger.info("NAS记录创建成功，ID: {}", nasEntity.getId());
            return nasEntity;
        } else {
            logger.error("NAS记录创建失败");
            throw new RuntimeException("创建NAS记录失败");
        }
    }

    @Override
    public boolean deleteNas(String id) {
        logger.info("删除NAS记录，ID: {}", id);

        // 检查记录是否存在
        NasEntity existing = nasMapper.findById(id);
        if (existing == null) {
            logger.warn("NAS记录不存在，ID: {}", id);
            throw new DataNotFoundException("NAS记录不存在，ID: " + id);
        }

        // 删除记录
        int result = nasMapper.deleteById(id);
        if (result > 0) {
            logger.info("NAS记录删除成功，ID: {}", id);
            return true;
        } else {
            logger.error("NAS记录删除失败，ID: {}", id);
            return false;
        }
    }

    @Override
    public NasEntity updateNas(NasEntity nasEntity) {
        logger.info("更新NAS记录: {}", nasEntity);

        // 验证必填字段
        if (nasEntity.getId() == null || nasEntity.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("NAS ID不能为空");
        }
        if (nasEntity.getNasName() == null || nasEntity.getNasName().trim().isEmpty()) {
            throw new IllegalArgumentException("NAS名称不能为空");
        }
        if (nasEntity.getNasIp() == null || nasEntity.getNasIp().trim().isEmpty()) {
            throw new IllegalArgumentException("NAS地址（JSON格式）不能为空");
        }
        
        // 验证JSON格式
        validateJsonFormat(nasEntity.getNasIp(), "NAS地址");

        // 检查记录是否存在
        NasEntity existing = nasMapper.findById(nasEntity.getId());
        if (existing == null) {
            logger.warn("NAS记录不存在，ID: {}", nasEntity.getId());
            throw new DataNotFoundException("NAS记录不存在，ID: " + nasEntity.getId());
        }

        // 检查NAS名称是否被其他记录使用
        List<NasEntity> existingByName = nasMapper.findByNasName(nasEntity.getNasName());
        if (!existingByName.isEmpty() && !existingByName.get(0).getId().equals(nasEntity.getId())) {
            throw new IllegalArgumentException("NAS名称已被其他记录使用: " + nasEntity.getNasName());
        }

        // 更新数据库
        int result = nasMapper.update(nasEntity);
        if (result > 0) {
            logger.info("NAS记录更新成功，ID: {}", nasEntity.getId());
            return nasMapper.findById(nasEntity.getId());
        } else {
            logger.error("NAS记录更新失败，ID: {}", nasEntity.getId());
            throw new RuntimeException("更新NAS记录失败");
        }
    }

    @Override
    public NasEntity getNasById(String id) {
        logger.debug("查询NAS记录，ID: {}", id);
        NasEntity nasEntity = nasMapper.findById(id);
        if (nasEntity == null) {
            logger.warn("NAS记录不存在，ID: {}", id);
            throw new DataNotFoundException("NAS记录不存在，ID: " + id);
        }
        return nasEntity;
    }

    @Override
    public List<NasEntity> getAllNas() {
        logger.debug("查询所有NAS记录");
        return nasMapper.findAll();
    }

    /**
     * 生成唯一ID
     * 格式: NAS_YYYYMMDD_HHMMSS_UUID前8位
     */
    private String generateUniqueId() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "NAS_" + timestamp + "_" + uuid;
    }
    
    /**
     * 验证JSON格式
     */
    private void validateJsonFormat(String jsonString, String fieldName) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        
        try {
            // 尝试解析JSON，验证格式是否正确
            objectMapper.readTree(jsonString);
        } catch (Exception e) {
            throw new IllegalArgumentException(fieldName + "必须是有效的JSON格式: " + e.getMessage());
        }
    }
}

