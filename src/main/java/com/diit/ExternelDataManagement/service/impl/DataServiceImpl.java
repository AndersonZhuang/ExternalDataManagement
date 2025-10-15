package com.diit.ExternelDataManagement.service.impl;

import com.diit.ExternelDataManagement.exception.DataNotFoundException;
import com.diit.ExternelDataManagement.mapper.DataMapper;
import com.diit.ExternelDataManagement.mapper2.workflow.WorkflowInstanceMapper;
import com.diit.ExternelDataManagement.pojo.DataEntity;
import com.diit.ExternelDataManagement.pojo.SyncStatusResult;
import com.diit.ExternelDataManagement.pojo.WorkflowInstanceEntity;
import com.diit.ExternelDataManagement.service.DataService;
import com.diit.ExternelDataManagement.service.WorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class DataServiceImpl implements DataService {

    private static final Logger logger = LoggerFactory.getLogger(DataServiceImpl.class);

    @Autowired
    private DataMapper dataMapper;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkflowInstanceMapper workflowInstanceMapper;

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
        dataEntity.setQualityStatus("质检中");
        dataEntity.setReceiveStatus("接收中");
        dataEntity.setInstanceId(String.valueOf(instanceId)); // 将Long类型的instanceId转换为String

        // 更新数据库
        logger.info("更新数据库状态字段，ID: {}, 接收代码: {}", id, receiveCode);
        dataMapper.updateStatusFields(dataEntity);

        // 返回更新后的数据
        logger.info("数据处理完成，ID: {}", id);
        return dataMapper.findById(id);
    }

    @Override
    public DataEntity restartQualityCheck(String id) {
        logger.info("开始重新质检，数据ID: {}", id);

        // 检查记录是否存在
        if (dataMapper.existsById(id) == 0) {
            logger.error("数据记录不存在，ID: {}", id);
            throw new DataNotFoundException("Data not found with id: " + id);
        }

        // 获取现有数据
        DataEntity existingData = dataMapper.findById(id);
        logger.debug("查询到现有数据: {}", existingData);

        // 检查是否已经接收过（必须有receive_code才能重新质检）
        if (existingData.getReceiveCode() == null || existingData.getReceiveCode().isEmpty()) {
            logger.error("数据尚未接收，无法重新质检，ID: {}", id);
            throw new IllegalStateException("Data has not been received yet, cannot restart quality check for id: " + id);
        }

        // 调用工作流API创建新的质检任务
        logger.info("开始调用工作流服务创建新的质检任务");
        Long instanceId = workflowService.startWorkflow();
        if (instanceId == null) {
            logger.error("工作流启动失败，数据ID: {}", id);
            throw new RuntimeException("Failed to start workflow for data id: " + id);
        }
        logger.info("工作流启动成功，新的实例ID: {}", instanceId);

        // 创建要更新的数据实体（更新instance_id和状态字段）
        DataEntity dataEntity = new DataEntity();
        dataEntity.setId(id);
        dataEntity.setInstanceId(String.valueOf(instanceId));

        // 更新数据库中的instance_id和状态
        logger.info("更新数据库instance_id和状态字段，ID: {}, 新实例ID: {}", id, instanceId);
        int updated = dataMapper.updateInstanceIdAndStatus(id, String.valueOf(instanceId), "质检中", "接收中");

        if (updated == 0) {
            logger.error("更新instance_id失败，ID: {}", id);
            throw new RuntimeException("Failed to update instance_id for data id: " + id);
        }

        // 返回更新后的数据
        logger.info("重新质检任务创建完成，ID: {}", id);
        return dataMapper.findById(id);
    }

    @Override
    public SyncStatusResult syncQualityCheckStatus() {
        logger.info("==================== 开始同步质检状态 ====================");

        SyncStatusResult result = new SyncStatusResult();

        // 查询所有质检中的记录
        List<DataEntity> qualityCheckingList = dataMapper.findAllInQualityCheck();
        result.setTotalCount(qualityCheckingList.size());
        logger.info("查询到 {} 条质检中的记录", qualityCheckingList.size());

        if (qualityCheckingList.isEmpty()) {
            logger.info("没有需要同步的质检记录");
            logger.info("==================== 同步完成 ====================");
            return result;
        }

        int updatedCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        // 遍历每条记录，查询工作流状态并更新
        for (DataEntity dataEntity : qualityCheckingList) {
            String instanceId = dataEntity.getInstanceId();
            String id = dataEntity.getId();
            String receiveCode = dataEntity.getReceiveCode();
            String oldQualityStatus = dataEntity.getQualityStatus();

            SyncStatusResult.SyncDetail detail = new SyncStatusResult.SyncDetail();
            detail.setId(id);
            detail.setReceiveCode(receiveCode);
            detail.setInstanceId(instanceId);
            detail.setOldQualityStatus(oldQualityStatus);

            logger.info("------------------------------------------------------");
            logger.info("处理记录 [{}] - ID: {}, ReceiveCode: {}, InstanceId: {}",
                    qualityCheckingList.indexOf(dataEntity) + 1, id, receiveCode, instanceId);

            try {
                // 查询工作流实例状态
                WorkflowInstanceEntity workflowInstance = workflowInstanceMapper.findById(Long.parseLong(instanceId));

                if (workflowInstance == null) {
                    logger.warn("❌ 未找到工作流实例，instance_id: {}", instanceId);
                    detail.setAction("failed");
                    detail.setMessage("未找到工作流实例");
                    failedCount++;
                    result.addDetail(detail);
                    continue;
                }

                Integer workflowStatus = workflowInstance.getStatus();
                detail.setWorkflowStatus(workflowStatus);

                String workflowStatusDesc = getWorkflowStatusDesc(workflowStatus);
                detail.setWorkflowStatusDesc(workflowStatusDesc);
                logger.info("工作流状态: {} ({})", workflowStatus, workflowStatusDesc);

                // 根据工作流状态更新质检状态
                // 2: 运行中 - 不修改
                // 3: 失败 - 修改为"未通过"
                // 4: 成功 - 修改为"通过"
                String newQualityStatus = null;

                if (workflowStatus == 2) {
                    logger.info("⏳ 工作流运行中，跳过更新");
                    detail.setAction("skipped");
                    detail.setMessage("工作流仍在运行中");
                    detail.setNewQualityStatus(oldQualityStatus);
                    skippedCount++;
                    result.addDetail(detail);
                    continue;
                } else if (workflowStatus == 3) {
                    newQualityStatus = "未通过";
                    logger.info("❌ 工作流失败，准备更新质检状态: {} -> {}", oldQualityStatus, newQualityStatus);
                } else if (workflowStatus == 4) {
                    newQualityStatus = "通过";
                    logger.info("✅ 工作流成功，准备更新质检状态: {} -> {}", oldQualityStatus, newQualityStatus);
                } else {
                    logger.warn("⚠️  未知的工作流状态: {}", workflowStatus);
                    detail.setAction("failed");
                    detail.setMessage("未知的工作流状态: " + workflowStatus);
                    failedCount++;
                    result.addDetail(detail);
                    continue;
                }

                detail.setNewQualityStatus(newQualityStatus);

                // 更新质检状态
                int updated = dataMapper.updateQualityStatusByInstanceId(instanceId, newQualityStatus);
                if (updated > 0) {
                    updatedCount++;
                    logger.info("✔️  成功更新质检状态: {} -> {}", oldQualityStatus, newQualityStatus);
                    detail.setAction("updated");
                    detail.setMessage("成功更新质检状态");
                } else {
                    logger.error("❌ 更新质检状态失败");
                    detail.setAction("failed");
                    detail.setMessage("数据库更新失败");
                    failedCount++;
                }
                result.addDetail(detail);

            } catch (NumberFormatException e) {
                logger.error("❌ instance_id 格式错误: {}", instanceId, e);
                detail.setAction("failed");
                detail.setMessage("instance_id格式错误: " + e.getMessage());
                failedCount++;
                result.addDetail(detail);
            } catch (Exception e) {
                logger.error("❌ 处理记录时发生异常，ID: {}, instance_id: {}", id, instanceId, e);
                detail.setAction("failed");
                detail.setMessage("异常: " + e.getMessage());
                failedCount++;
                result.addDetail(detail);
            }
        }

        result.setUpdatedCount(updatedCount);
        result.setSkippedCount(skippedCount);
        result.setFailedCount(failedCount);

        logger.info("======================================================");
        logger.info("质检状态同步完成!");
        logger.info("总记录数: {}", result.getTotalCount());
        logger.info("成功更新: {} 条", updatedCount);
        logger.info("跳过记录: {} 条 (工作流运行中)", skippedCount);
        logger.info("失败记录: {} 条", failedCount);
        logger.info("==================== 同步结束 ====================");

        return result;
    }

    private String getWorkflowStatusDesc(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 1: return "等待调度";
            case 2: return "运行中";
            case 3: return "失败";
            case 4: return "成功";
            case 5: return "取消";
            case 10: return "等待手工触发";
            default: return "未知状态";
        }
    }

    private String generateUniqueReceiveCode() {
        // 生成格式: REC_YYYYMMDD_HHMMSS_UUID前8位
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "REC_" + timestamp + "_" + uuid;
    }
}