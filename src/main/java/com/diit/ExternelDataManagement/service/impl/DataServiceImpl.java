package com.diit.ExternelDataManagement.service.impl;

import com.diit.ExternelDataManagement.exception.DataNotFoundException;
import com.diit.ExternelDataManagement.mapper.DataMapper;
import com.diit.ExternelDataManagement.pojo.DataEntity;
import com.diit.ExternelDataManagement.service.DataService;
import com.diit.ExternelDataManagement.service.WorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class DataServiceImpl implements DataService {

    private static final Logger logger = LoggerFactory.getLogger(DataServiceImpl.class);

    @Autowired
    private DataMapper dataMapper;

    @Autowired
    private WorkflowService workflowService;

    @Override
    public DataEntity processDataById(String id) {
        logger.info("开始处理数据ID: {}", id);

        // 检查记录是否存在
        if (dataMapper.existsById(id) == 0) {
            logger.error("数据记录不存在，ID: {}", id);
            throw new DataNotFoundException("Data not found with id: " + id);
        }
        logger.debug("数据记录存在确认，ID: {}", id);

        // 生成唯一的receive_code
        String receiveCode = generateUniqueReceiveCode();
        logger.info("生成接收代码: {}", receiveCode);

        // 获取当前时间作为receive_time
        LocalDateTime receiveTime = LocalDateTime.now();
        logger.debug("接收时间: {}", receiveTime);

        // 调用工作流API创建质检任务
        logger.info("开始调用工作流服务创建质检任务");
        Long instanceId = workflowService.startWorkflow();
        if (instanceId == null) {
            logger.error("工作流启动失败，数据ID: {}", id);
            throw new RuntimeException("Failed to start workflow for data id: " + id);
        }
        logger.info("工作流启动成功，实例ID: {}", instanceId);

        // 创建要更新的数据实体
        DataEntity dataEntity = new DataEntity();
        dataEntity.setId(id);
        dataEntity.setReceiveCode(receiveCode);
        dataEntity.setReceiveTime(receiveTime);
        dataEntity.setQualityStatus("未通过");
        dataEntity.setReceiveStatus("接收中");
        dataEntity.setInstanceId(String.valueOf(instanceId)); // 将Long类型的instanceId转换为String

        // 更新数据库
        logger.info("更新数据库状态字段，ID: {}, 接收代码: {}", id, receiveCode);
        dataMapper.updateStatusFields(dataEntity);

        // 返回更新后的数据
        logger.info("数据处理完成，ID: {}", id);
        return dataMapper.findById(id);
    }

    private String generateUniqueReceiveCode() {
        // 生成格式: REC_YYYYMMDD_HHMMSS_UUID前8位
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "REC_" + timestamp + "_" + uuid;
    }
}