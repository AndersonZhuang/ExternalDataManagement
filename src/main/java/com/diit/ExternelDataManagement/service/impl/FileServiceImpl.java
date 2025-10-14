package com.diit.ExternelDataManagement.service.impl;

import com.diit.ExternelDataManagement.exception.DataNotFoundException;
import com.diit.ExternelDataManagement.mapper.DataMapper;
import com.diit.ExternelDataManagement.mapper.FileMapper;
import com.diit.ExternelDataManagement.pojo.FileEntity;
import com.diit.ExternelDataManagement.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private DataMapper dataMapper;

    @Override
    public List<FileEntity> parseAndSaveFiles(String receiveCode) {
        // 根据receiveCode查询文件路径
        String filePath = fileMapper.getFilePathByReceiveCode(receiveCode);
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new DataNotFoundException("File path not found for receive code: " + receiveCode);
        }

        List<FileEntity> fileEntities = new ArrayList<>();
        File directory = new File(filePath);

        if (!directory.exists() || !directory.isDirectory()) {
            throw new RuntimeException("Directory does not exist or is not a directory: " + filePath);
        }

        // 递归解析目录下的所有文件
        parseDirectoryRecursively(directory, receiveCode, fileEntities);

        // 批量保存到数据库
        for (FileEntity fileEntity : fileEntities) {
            fileMapper.insert(fileEntity);
        }

        // 更新receive_external_package_info表的状态字段
        dataMapper.updateStatusByReceiveCode(receiveCode, "通过", "已接收");

        return fileEntities;
    }

    private void parseDirectoryRecursively(File directory, String receiveCode, List<FileEntity> fileEntities) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // 递归处理子目录
                    parseDirectoryRecursively(file, receiveCode, fileEntities);
                } else {
                    // 处理文件
                    FileEntity fileEntity = createFileEntity(file, receiveCode);
                    fileEntities.add(fileEntity);
                }
            }
        }
    }

    private FileEntity createFileEntity(File file, String receiveCode) {
        FileEntity fileEntity = new FileEntity();

        // 生成唯一ID
        fileEntity.setId(generateUniqueId());
        fileEntity.setReceiveCode(receiveCode);
        fileEntity.setFilePath(file.getAbsolutePath());

        // 根据文件扩展名判断数据类型
        String fileName = file.getName();
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            String extension = fileName.substring(lastDotIndex + 1).toLowerCase();
            fileEntity.setDataType(extension);
        } else {
            fileEntity.setDataType("unknown");
        }

        // 设置文件大小（转换为可读格式）
        long fileSizeBytes = file.length();
        fileEntity.setDataSize(formatFileSize(fileSizeBytes));

        // 设置默认值
        fileEntity.setBbox(null); // bbox默认为空
        fileEntity.setTotalObjectNum(0); // 默认对象数量为0
        fileEntity.setTotalArea(0.0); // 默认面积为0
        fileEntity.setLayerName(getFileNameWithoutExtension(fileName)); // 使用文件名作为图层名

        return fileEntity;
    }

    private String generateUniqueId() {
        // 生成格式: FILE_YYYYMMDD_HHMMSS_UUID前8位
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "FILE_" + timestamp + "_" + uuid;
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + "B";
        if (bytes < 1024 * 1024) return String.format("%.1fKB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1fMB", bytes / (1024.0 * 1024.0));
        return String.format("%.1fGB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    private String getFileNameWithoutExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(0, lastDotIndex);
        }
        return fileName;
    }
}