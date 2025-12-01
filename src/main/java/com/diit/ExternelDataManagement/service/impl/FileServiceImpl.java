package com.diit.ExternelDataManagement.service.impl;

import com.diit.ExternelDataManagement.exception.DataNotFoundException;
import com.diit.ExternelDataManagement.mapper.DataMapper;
import com.diit.ExternelDataManagement.mapper.FileMapper;
import com.diit.ExternelDataManagement.pojo.FileEntity;
import com.diit.ExternelDataManagement.pojo.LayerInfo;
import com.diit.ExternelDataManagement.service.FileService;
import com.diit.ExternelDataManagement.service.GeoSpatialParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);

    // 支持的数据文件扩展名
    private static final Set<String> SUPPORTED_EXTENSIONS = new HashSet<>(
            Arrays.asList("shp", "gdb", "mdb", "tiff", "tif", "img", "doc", "docx", "xls", "xlsx")
    );

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private DataMapper dataMapper;

    @Autowired
    private GeoSpatialParser geoSpatialParser;
    
    /**
     * 规范化文件路径，支持 UNC 路径
     * @param filePath 原始路径
     * @return 规范化后的路径
     */
    private String normalizePath(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return filePath;
        }
        // 去除首尾空格
        String trimmed = filePath.trim();
        
        // 处理 UNC 路径：确保格式为 \\server\share\path
        // 如果路径以 \\ 开头，规范化 UNC 路径
        if (trimmed.startsWith("\\\\")) {
            // UNC 路径格式：\\server\share\path
            // 将多个连续的反斜杠合并为两个（但保留 UNC 路径开头的两个反斜杠）
            // 例如：\\\\server\\share -> \\server\share
            String normalized = trimmed;
            // 如果开头有超过两个反斜杠，只保留两个
            if (normalized.startsWith("\\\\\\")) {
                normalized = "\\\\" + normalized.substring(normalized.indexOf("\\", 2));
            }
            // 确保路径中间的反斜杠格式正确（不合并 UNC 路径开头的两个反斜杠）
            // 将路径中除了开头的两个反斜杠外的多个连续反斜杠合并为一个
            if (normalized.length() > 2) {
                String prefix = normalized.substring(0, 2); // 保留开头的 \\
                String rest = normalized.substring(2);
                rest = rest.replaceAll("\\\\+", "\\\\"); // 合并多个反斜杠为一个
                normalized = prefix + rest;
            }
            logger.debug("UNC 路径规范化: {} -> {}", trimmed, normalized);
            return normalized;
        }
        
        // 普通路径：Java 的 File 和 Paths 类可以处理
        return trimmed;
    }

    @Override
    public List<FileEntity> parseAndSaveFiles(String receiveCode) {
        // 根据receiveCode查询文件路径
        String filePath = fileMapper.getFilePathByReceiveCode(receiveCode);
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new DataNotFoundException("File path not found for receive code: " + receiveCode);
        }

        List<FileEntity> fileEntities = new ArrayList<>();
        
        // 规范化路径（处理 UNC 路径）
        String normalizedPath = normalizePath(filePath);
        logger.info("原始路径: {}, 规范化后路径: {}", filePath, normalizedPath);
        
        // 使用 NIO Path API 检查路径，对 UNC 路径支持更好
        Path path = Paths.get(normalizedPath);
        File directory = path.toFile();
        
        logger.debug("尝试访问路径: {}", normalizedPath);
        logger.debug("Path 对象: {}", path);
        logger.debug("File 对象: {}", directory.getAbsolutePath());
        
        // 检查路径是否存在（先尝试 NIO API，再尝试 File API）
        boolean exists = false;
        boolean isDirectory = false;
        
        try {
            // 尝试使用 NIO API 检查
            exists = Files.exists(path);
            logger.debug("NIO Files.exists() 结果: {}", exists);
            if (exists) {
                isDirectory = Files.isDirectory(path);
                logger.debug("NIO Files.isDirectory() 结果: {}", isDirectory);
            }
        } catch (Exception e) {
            logger.warn("使用 NIO API 检查路径失败: {} - {}", e.getClass().getSimpleName(), e.getMessage());
        }
        
        // 如果 NIO API 失败，回退到 File API
        if (!exists) {
            try {
                exists = directory.exists();
                logger.debug("File.exists() 结果: {}", exists);
                if (exists) {
                    isDirectory = directory.isDirectory();
                    logger.debug("File.isDirectory() 结果: {}", isDirectory);
                }
            } catch (Exception e) {
                logger.warn("使用 File API 检查路径失败: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            }
        }
        
        // 对于 UNC 路径，如果两种方法都失败，尝试列出父目录
        if (!exists && normalizedPath.startsWith("\\\\")) {
            try {
                // 尝试访问父目录或共享根目录
                int firstSlashIndex = normalizedPath.indexOf('\\', 2);
                if (firstSlashIndex > 0) {
                    String shareRoot = normalizedPath.substring(0, firstSlashIndex + 1);
                    logger.debug("尝试访问共享根目录: {}", shareRoot);
                    File shareRootFile = new File(shareRoot);
                    if (shareRootFile.exists()) {
                        logger.info("共享根目录可访问: {}", shareRoot);
                        // 共享根可访问，但目标路径不可访问，可能是路径不正确
                    } else {
                        logger.warn("共享根目录不可访问: {}", shareRoot);
                    }
                }
            } catch (Exception e) {
                logger.debug("测试共享根目录访问失败: {}", e.getMessage());
            }
        }
        
        // 如果路径不存在，提供详细的错误信息
        if (!exists) {
            String errorMsg = "目录不存在: " + filePath;
            // 如果是 UNC 路径，提供额外的诊断信息
            if (normalizedPath.startsWith("\\\\")) {
                logger.error("═══════════════════════════════════════════════════════════");
                logger.error("UNC 路径访问失败: {}", normalizedPath);
                logger.error("可能的原因：");
                logger.error("1. 路径格式不正确（应为 \\\\server\\share\\path 格式）");
                logger.error("2. 网络连接问题或 NAS 服务器不可达");
                logger.error("3. 需要身份验证但未提供凭据");
                logger.error("4. 共享权限不足");
                logger.error("5. 防火墙阻止了网络访问");
                logger.error("建议解决方案：");
                logger.error("1. 在 Windows 文件管理器中手动访问该路径，确认可以打开");
                logger.error("2. 如果无法访问，尝试映射网络驱动器：");
                logger.error("   - 打开文件管理器 -> 此电脑 -> 映射网络驱动器");
                logger.error("   - 选择驱动器盘符（如 Z:）");
                logger.error("   - 输入 UNC 路径: {}", normalizedPath);
                logger.error("   - 勾选\"登录时重新连接\"（如果需要）");
                logger.error("   - 如果提示输入凭据，请输入 NAS 的用户名和密码");
                logger.error("3. 映射成功后，使用映射的驱动器路径（如 Z:\\SDZJDS）代替 UNC 路径");
                logger.error("═══════════════════════════════════════════════════════════");
                errorMsg += " (UNC 路径，请确保网络连接正常且路径可访问)";
            }
            throw new RuntimeException(errorMsg);
        }
        
        // 检查是否为目录
        if (!isDirectory) {
            throw new RuntimeException("路径不是目录: " + filePath);
        }
        
        logger.info("目录验证通过: {}", normalizedPath);

        // 递归解析目录下的所有文件
        parseDirectoryRecursively(directory, receiveCode, fileEntities);

        // 批量保存到数据库，添加错误处理和重复检查
        if (!fileEntities.isEmpty()) {
            // 先查询已存在的记录，避免重复插入
            List<FileEntity> existingFiles = fileMapper.findByReceiveCode(receiveCode);
            Set<String> existingKeys = new HashSet<>();
            for (FileEntity existing : existingFiles) {
                String key = existing.getFilePath() + "|" + (existing.getLayerName() != null ? existing.getLayerName() : "");
                existingKeys.add(key);
            }
            
            int successCount = 0;
            int skipCount = 0;
            int failCount = 0;
            
            for (FileEntity fileEntity : fileEntities) {
                try {
                    // 检查是否已存在相同的记录（根据文件路径和图层名称）
                    String key = fileEntity.getFilePath() + "|" + (fileEntity.getLayerName() != null ? fileEntity.getLayerName() : "");
                    
                    if (existingKeys.contains(key)) {
                        skipCount++;
                        logger.debug("文件记录已存在，跳过插入: 文件路径={}, 图层={}", 
                                fileEntity.getFilePath(), fileEntity.getLayerName());
                        continue;
                    }
                    
                    // 执行插入
                    int result = fileMapper.insert(fileEntity);
                    if (result > 0) {
                        successCount++;
                        existingKeys.add(key); // 添加到已存在集合，避免同批次重复
                        logger.debug("成功插入文件记录: ID={}, 文件路径={}", 
                                fileEntity.getId(), fileEntity.getFilePath());
                    } else {
                        failCount++;
                        logger.warn("插入文件记录返回0，可能插入失败: 文件路径={}", fileEntity.getFilePath());
                    }
                } catch (Exception e) {
                    failCount++;
                    logger.error("插入文件记录失败: 文件路径={}, 图层={}, 错误: {}", 
                            fileEntity.getFilePath(), fileEntity.getLayerName(), e.getMessage(), e);
                    // 继续处理其他文件，不中断整个流程
                }
            }
            
            logger.info("文件保存完成 - 成功: {} 条，跳过: {} 条，失败: {} 条，总计: {} 条", 
                    successCount, skipCount, failCount, fileEntities.size());
            
            // 如果所有文件都保存失败，抛出异常
            if (successCount == 0 && skipCount == 0 && !fileEntities.isEmpty()) {
                throw new RuntimeException("所有文件记录保存失败，请检查日志");
            }
        } else {
            logger.warn("未找到任何可解析的文件，receiveCode: {}", receiveCode);
        }

        // 更新receive_external_package_info表的状态字段（仅在至少有一条记录成功保存后）
        try {
            dataMapper.updateStatusByReceiveCode(receiveCode, "通过", "已接收");
            logger.info("成功更新状态字段，receiveCode: {}", receiveCode);
        } catch (Exception e) {
            logger.error("更新状态字段失败，receiveCode: {}, 错误: {}", receiveCode, e.getMessage(), e);
            // 状态更新失败不应该影响整个流程，只记录错误
        }

        return fileEntities;
    }

    private void parseDirectoryRecursively(File directory, String receiveCode, List<FileEntity> fileEntities) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // 检查是否是 .gdb 文件夹（Geodatabase 是一个文件夹，但名称以 .gdb 结尾）
                    String fileName = file.getName().toLowerCase();
                    if (fileName.endsWith(".gdb")) {
                        // GDB 文件夹直接作为整体解析，不递归进入
                        logger.info("发现 GDB 文件夹: {}", file.getAbsolutePath());
                        parseFileWithParser(file, receiveCode, fileEntities);
                    } else {
                        // 其他文件夹递归处理子目录
                        parseDirectoryRecursively(file, receiveCode, fileEntities);
                    }
                } else {
                    // 只处理支持的文件类型
                    if (isSupportedFile(file)) {
                        // 使用地理空间解析器解析文件，为每个图层创建一条记录
                        parseFileWithParser(file, receiveCode, fileEntities);
                    }
                }
            }
        }
    }

    /**
     * 使用地理空间解析器解析文件，提取图层信息
     * @param file 文件对象
     * @param receiveCode 接收编码
     * @param fileEntities 文件实体列表
     */
    private void parseFileWithParser(File file, String receiveCode, List<FileEntity> fileEntities) {
        try {
            logger.info("使用地理空间解析器解析文件: {}", file.getAbsolutePath());
            
            // 检查是否是容器文件（GDB、MDB等）
            boolean isContainerFile = isContainerFile(file);
            
            // 使用地理空间解析器解析图层信息
            List<LayerInfo> layerInfos = geoSpatialParser.parseLayers(file.getAbsolutePath());
            
            if (layerInfos != null && !layerInfos.isEmpty()) {
                // 如果是容器文件，先为容器文件本身创建一条记录
                if (isContainerFile) {
                    FileEntity containerEntity = createContainerFileEntity(file, receiveCode, layerInfos);
                    fileEntities.add(containerEntity);
                    logger.info("添加容器文件记录: {}, 包含 {} 个图层", file.getName(), layerInfos.size());
                }
                
                // 为每个图层创建一条记录
                for (LayerInfo layerInfo : layerInfos) {
                    FileEntity fileEntity = createFileEntityFromLayer(file, receiveCode, layerInfo);
                    fileEntities.add(fileEntity);
                    logger.info("添加图层记录: {}, 要素数量: {}", layerInfo.getLayerName(), layerInfo.getFeatureCount());
                }
            } else {
                // 如果没有解析到图层信息，创建一个默认记录
                logger.warn("未解析到图层信息，创建默认记录: {}", file.getAbsolutePath());
                FileEntity fileEntity = createDefaultFileEntity(file, receiveCode);
                fileEntities.add(fileEntity);
            }
        } catch (Exception e) {
            logger.error("地理空间解析文件失败: {}, 创建默认记录", file.getAbsolutePath(), e);
            // 解析失败时，创建一个默认记录
            FileEntity fileEntity = createDefaultFileEntity(file, receiveCode);
            fileEntities.add(fileEntity);
        }
    }

    /**
     * 检查文件是否是支持的类型
     * @param file 文件对象
     * @return true表示支持，false表示不支持
     */
    private boolean isSupportedFile(File file) {
        String fileName = file.getName().toLowerCase();
        int lastDotIndex = fileName.lastIndexOf('.');
        
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            String extension = fileName.substring(lastDotIndex + 1);
            return SUPPORTED_EXTENSIONS.contains(extension);
        }
        
        return false;
    }

    /**
     * 根据图层信息创建文件实体
     * @param file 文件对象
     * @param receiveCode 接收编码
     * @param layerInfo 图层信息
     * @return 文件实体
     */
    private FileEntity createFileEntityFromLayer(File file, String receiveCode, LayerInfo layerInfo) {
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

        // 设置文件类型（矢量/栅格/文档）
        String fileType = determineFileType(layerInfo.getGeometryType(), fileEntity.getDataType());
        fileEntity.setFileType(fileType);

        // 设置文件大小（转换为可读格式）
        // 对于 GDB 图层，使用图层对应的 .gdbtable 文件大小
        // 对于文件夹（如 GDB），需要计算文件夹的总大小
        // 对于普通文件，使用文件大小
        long fileSizeBytes;
        if (layerInfo.getFilePath() != null && !layerInfo.getFilePath().isEmpty()) {
            // GDB 图层有指定的文件路径，使用该文件的大小
            File layerFile = new File(layerInfo.getFilePath());
            if (layerFile.exists() && layerFile.isFile()) {
                fileSizeBytes = layerFile.length();
            } else {
                // 如果指定的文件不存在，回退到原逻辑
                if (file.isDirectory()) {
                    fileSizeBytes = calculateDirectorySize(file);
                } else {
                    fileSizeBytes = file.length();
                }
            }
        } else if (file.isDirectory()) {
            // 文件夹但没有指定图层文件路径，计算文件夹的总大小
            fileSizeBytes = calculateDirectorySize(file);
        } else {
            // 普通文件，使用文件大小
            fileSizeBytes = file.length();
        }
        fileEntity.setDataSize(formatFileSize(fileSizeBytes));

        // 使用解析器的图层信息填充字段
        fileEntity.setLayerName(layerInfo.getLayerName()); // 图层名称
        fileEntity.setTotalObjectNum(layerInfo.getFeatureCount()); // 要素数量
        fileEntity.setBbox(layerInfo.getBbox()); // 边界框
        fileEntity.setTotalArea(layerInfo.getTotalArea()); // 总面积

        return fileEntity;
    }

    /**
     * 创建默认文件实体（当GDAL解析失败时）
     * @param file 文件对象
     * @param receiveCode 接收编码
     * @return 文件实体
     */
    private FileEntity createDefaultFileEntity(File file, String receiveCode) {
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

        // 设置文件类型（矢量/栅格/文档）
        String fileType = determineFileType(null, fileEntity.getDataType());
        fileEntity.setFileType(fileType);

        // 设置文件大小（转换为可读格式）
        // 对于文件夹（如 GDB），需要计算文件夹的总大小
        long fileSizeBytes;
        if (file.isDirectory()) {
            fileSizeBytes = calculateDirectorySize(file);
        } else {
            fileSizeBytes = file.length();
        }
        fileEntity.setDataSize(formatFileSize(fileSizeBytes));

        // 设置默认值
        fileEntity.setBbox(null); // bbox默认为空
        fileEntity.setTotalObjectNum(0); // 默认对象数量为0
        fileEntity.setTotalArea(0.0); // 默认面积为0
        fileEntity.setLayerName(getFileNameWithoutExtension(fileName)); // 使用文件名作为图层名

        return fileEntity;
    }

    /**
     * 计算文件夹的总大小（递归计算所有文件的大小）
     * @param directory 文件夹对象
     * @return 文件夹的总大小（字节）
     */
    private long calculateDirectorySize(File directory) {
        long size = 0;
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        size += file.length();
                    } else if (file.isDirectory()) {
                        size += calculateDirectorySize(file); // 递归计算子文件夹大小
                    }
                }
            }
        }
        return size;
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

    /**
     * 确定文件类型（使用实际文件格式）
     * @param geometryType 几何类型（来自解析器）
     * @param dataType 数据类型（文件扩展名）
     * @return 文件类型：实际的文件格式（gdb、shp、tiff等）
     */
    private String determineFileType(String geometryType, String dataType) {
        // 根据文件扩展名判断
        if (dataType == null) {
            return "unknown";
        }
        
        String lowerDataType = dataType.toLowerCase();
        
        // 直接返回文件扩展名作为文件类型
        // Office文档类型
        if (lowerDataType.equals("doc") || lowerDataType.equals("docx") || 
            lowerDataType.equals("xls") || lowerDataType.equals("xlsx")) {
            return lowerDataType;
        }
        
        // 栅格数据类型
        if (lowerDataType.equals("tiff") || lowerDataType.equals("tif") || lowerDataType.equals("img")) {
            return lowerDataType;
        }
        
        // 矢量数据类型 - 直接返回格式名称
        if (lowerDataType.equals("shp") || lowerDataType.equals("gdb") || lowerDataType.equals("mdb")) {
            return lowerDataType;
        }
        
        // 根据geometryType判断
        if (geometryType != null) {
            String lowerGeometryType = geometryType.toLowerCase();
            if (lowerGeometryType.contains("raster")) {
                return "raster";
            }
            if (lowerGeometryType.contains("document")) {
                return "document";
            }
        }
        
        return "unknown";
    }

    /**
     * 检查文件是否是容器文件（包含多个图层的文件格式）
     * @param file 文件对象
     * @return true表示是容器文件，false表示不是
     */
    private boolean isContainerFile(File file) {
        String fileName = file.getName().toLowerCase();
        // GDB 是文件夹，MDB 是文件
        return fileName.endsWith(".gdb") || fileName.endsWith(".mdb");
    }

    /**
     * 为容器文件创建文件实体（汇总信息）
     * @param file 文件对象
     * @param receiveCode 接收编码
     * @param layerInfos 图层信息列表
     * @return 容器文件实体
     */
    private FileEntity createContainerFileEntity(File file, String receiveCode, List<LayerInfo> layerInfos) {
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
            fileEntity.setFileType(extension); // 使用实际文件格式
        } else {
            fileEntity.setDataType("unknown");
            fileEntity.setFileType("unknown");
        }

        // 计算容器文件的总大小
        long fileSizeBytes;
        if (file.isDirectory()) {
            fileSizeBytes = calculateDirectorySize(file);
        } else {
            fileSizeBytes = file.length();
        }
        fileEntity.setDataSize(formatFileSize(fileSizeBytes));

        // 汇总所有图层的要素数量
        int totalFeatures = 0;
        for (LayerInfo layerInfo : layerInfos) {
            totalFeatures += layerInfo.getFeatureCount();
        }

        // 设置汇总信息
        fileEntity.setBbox(null); // 容器文件不设置具体的边界框
        fileEntity.setTotalObjectNum(totalFeatures); // 所有图层的要素总数
        fileEntity.setTotalArea(0.0); // 容器文件不计算面积
        fileEntity.setLayerName(getFileNameWithoutExtension(fileName) + " (容器)"); // 标识为容器文件

        return fileEntity;
    }
}