package com.diit.ExternelDataManagement.service.impl;

import com.diit.ExternelDataManagement.pojo.LayerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 简化的地理空间数据解析器
 * 支持 GDB (通过 SQLite) 和 Shapefile (基本解析)
 * 
 * @author Assistant
 * @since 2025-11-05
 */
@Component
public class SimpleGeoParser {
    
    private static final Logger logger = LoggerFactory.getLogger(SimpleGeoParser.class);
    
    /**
     * 规范化文件路径，支持多种路径格式
     * 支持：
     * - Windows 普通路径：C:\data\file.gdb
     * - Windows UNC 路径：\\server\share\file.gdb
     * - Unix/Linux 路径：/data/file.gdb
     * @param filePath 原始路径
     * @return 规范化后的路径（Java File 和 Paths 类可以直接使用）
     */
    private String normalizePath(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return filePath;
        }
        // Java 的 File 和 Paths 类可以处理各种路径格式
        // 保持路径原样，让 Java 的文件系统 API 自动处理
        return filePath;
    }
    
    /**
     * 为 SQLite JDBC URL 规范化路径
     * SQLite JDBC 需要将反斜杠转换为正斜杠
     * 支持：
     * - Windows 普通路径：C:\data\file.gdb -> C:/data/file.gdb
     * - Windows UNC 路径：\\server\share\file.gdb -> //server/share/file.gdb
     * - Unix/Linux 路径：/data/file.gdb（保持不变）
     * @param filePath 文件路径
     * @return 适用于 SQLite JDBC 的路径（使用正斜杠）
     */
    private String normalizePathForSqlite(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return filePath;
        }
        // SQLite JDBC 在 Windows 和 Unix 系统上都能处理正斜杠路径
        // 将反斜杠统一转换为正斜杠，确保跨平台兼容性
        // 对于 UNC 路径（\\server\share），转换为 //server/share
        // 对于 Windows 普通路径（C:\data），转换为 C:/data
        return filePath.replace('\\', '/');
    }
    
    /**
     * 解析地理空间数据文件
     */
    public List<LayerInfo> parseGeospatialFile(String filePath) throws IOException {
        logger.info("解析地理空间文件: {}", filePath);
        
        // 规范化路径
        String normalizedPath = normalizePath(filePath);
        File file = new File(normalizedPath);
        if (!file.exists()) {
            throw new IOException("文件不存在: " + filePath);
        }
        
        // 根据文件类型选择解析方法
        if (file.isDirectory() && normalizedPath.toLowerCase().endsWith(".gdb")) {
            return parseGdbFile(normalizedPath);
        } else if (normalizedPath.toLowerCase().endsWith(".shp")) {
            return parseShapefileBasic(normalizedPath);
        } else {
            throw new IOException("暂不支持的文件格式: " + filePath + "，当前支持 .gdb 和 .shp");
        }
    }
    
    /**
     * 解析 GDB 文件（通过 SQLite JDBC）
     */
    private List<LayerInfo> parseGdbFile(String gdbPath) throws IOException {
        logger.info("解析 GDB 文件: {}", gdbPath);
        
        List<LayerInfo> layers = new ArrayList<>();
        
        try {
            // 查找 GDB 目录中的表文件
            Path gdbDir = Paths.get(gdbPath);
            logger.debug("GDB 目录路径: {}", gdbDir.toAbsolutePath());
            
            if (!Files.exists(gdbDir)) {
                throw new IOException("GDB 目录不存在: " + gdbPath);
            }
            
            if (!Files.isDirectory(gdbDir)) {
                throw new IOException("路径不是目录: " + gdbPath);
            }
            
            List<Path> tableFiles = Files.walk(gdbDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String fileName = path.getFileName().toString().toLowerCase();
                        return fileName.startsWith("a") && fileName.matches("a[0-9]{8}\\.gdbtable");
                    })
                    .collect(Collectors.toList());
            
            logger.info("发现 {} 个 GDB 表文件", tableFiles.size());
            
            if (tableFiles.isEmpty()) {
                logger.warn("未找到任何 GDB 表文件，目录可能不是有效的 GDB 格式: {}", gdbPath);
                throw new IOException("未找到任何 GDB 表文件: " + gdbPath);
            }
            
            // 尝试通过 SQLite 读取 GDB 元数据
            Map<String, LayerMetadata> layerMetadataMap = readGdbMetadata(gdbPath);
            
            // 为每个表文件创建图层信息
            for (int i = 0; i < tableFiles.size(); i++) {
                Path tableFile = tableFiles.get(i);
                String fileName = tableFile.getFileName().toString();
                String tableId = fileName.substring(0, fileName.lastIndexOf('.'));
                
                LayerInfo layer = new LayerInfo();
                
                // 尝试获取真实的图层信息
                LayerMetadata metadata = layerMetadataMap.get(tableId);
                if (metadata != null) {
                    layer.setLayerName(metadata.name);
                    layer.setFeatureCount(metadata.featureCount);
                    layer.setGeometryType(metadata.geometryType);
                } else {
                    layer.setLayerName("图层_" + (i + 1));
                    // 基于文件大小估算要素数量
                    long fileSize = Files.size(tableFile);
                    int estimatedCount = Math.max(1, (int) (fileSize / 512)); // 粗略估计
                    layer.setFeatureCount(estimatedCount);
                    layer.setGeometryType("Unknown");
                }
                
                layer.setFilePath(tableFile.toString()); // 使用单个表文件路径，而不是整个 GDB 目录
                layer.setBbox(null);
                layer.setTotalArea(0.0);
                
                layers.add(layer);
                logger.info("解析图层: {} - {} 个要素", layer.getLayerName(), layer.getFeatureCount());
            }
            
        } catch (Exception e) {
            logger.error("解析 GDB 文件失败: {}", e.getMessage(), e);
            throw new IOException("解析 GDB 文件失败: " + e.getMessage(), e);
        }
        
        return layers;
    }
    
    /**
     * 尝试读取 GDB 元数据
     */
    private Map<String, LayerMetadata> readGdbMetadata(String gdbPath) {
        Map<String, LayerMetadata> metadataMap = new HashMap<>();
        
        try {
            Path gdbDir = Paths.get(gdbPath);
            
            // 1. 首先尝试读取系统目录表 (a00000001.gdbtable)
            Map<String, String> tableNameMap = readGdbCatalog(gdbDir);
            
            // 2. 然后读取每个表的记录数
            List<Path> tableFiles = Files.walk(gdbDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String fileName = path.getFileName().toString().toLowerCase();
                        return fileName.startsWith("a") && fileName.matches("a[0-9]{8}\\.gdbtable");
                    })
                    .collect(Collectors.toList());
            
            for (Path tableFile : tableFiles) {
                String fileName = tableFile.getFileName().toString();
                String tableId = fileName.substring(0, fileName.lastIndexOf('.'));
                
                // 尝试通过 SQLite 读取记录数
                try {
                    // 规范化路径以支持 UNC 路径
                    String sqlitePath = normalizePathForSqlite(tableFile.toString());
                    String jdbcUrl = "jdbc:sqlite:" + sqlitePath;
                    logger.debug("尝试连接 SQLite: {}", jdbcUrl);
                    
                    try (Connection conn = DriverManager.getConnection(jdbcUrl);
                         Statement stmt = conn.createStatement()) {
                        
                        // 查询表结构
                        ResultSet tables = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table'");
                        while (tables.next()) {
                            String tableName = tables.getString("name");
                            
                            // 跳过系统表
                            if (tableName.startsWith("sqlite_") || tableName.startsWith("rtree_")) {
                                continue;
                            }
                            
                            try {
                                ResultSet countRs = stmt.executeQuery("SELECT COUNT(*) FROM \"" + tableName + "\"");
                                if (countRs.next()) {
                                    int count = countRs.getInt(1);
                                    
                                    LayerMetadata metadata = new LayerMetadata();
                                    // 使用目录中的真实名称，如果没有则使用表名
                                    metadata.name = tableNameMap.getOrDefault(tableId, tableName);
                                    metadata.featureCount = count;
                                    metadata.geometryType = "Unknown";
                                    
                                    metadataMap.put(tableId, metadata);
                                    logger.debug("表 {} (ID: {}) 有 {} 条记录", metadata.name, tableId, count);
                                }
                                countRs.close();
                            } catch (Exception e) {
                                logger.warn("无法查询表 {} 的记录数: {}", tableName, e.getMessage());
                            }
                        }
                        tables.close();
                    }
                } catch (Exception e) {
                    logger.warn("无法读取表文件 {}: {} - 将使用估算值", tableFile, e.getMessage());
                    if (logger.isDebugEnabled()) {
                        logger.debug("读取表文件异常详情", e);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.warn("读取 GDB 元数据失败: {} - 将使用默认图层名称", e.getMessage());
            if (logger.isDebugEnabled()) {
                logger.debug("读取 GDB 元数据异常详情", e);
            }
        }
        
        return metadataMap;
    }
    
    /**
     * 读取 GDB 目录表，获取表ID到表名的映射
     */
    private Map<String, String> readGdbCatalog(Path gdbDir) {
        Map<String, String> tableNameMap = new HashMap<>();
        
        try {
            // GDB 目录表通常是 a00000001.gdbtable
            Path catalogFile = gdbDir.resolve("a00000001.gdbtable");
            if (!Files.exists(catalogFile)) {
                logger.debug("未找到 GDB 目录表文件");
                return tableNameMap;
            }
            
            // 规范化路径以支持 UNC 路径
            String sqlitePath = normalizePathForSqlite(catalogFile.toString());
            String jdbcUrl = "jdbc:sqlite:" + sqlitePath;
            logger.debug("尝试连接 GDB 目录表 SQLite: {}", jdbcUrl);
            
            try (Connection conn = DriverManager.getConnection(jdbcUrl);
                 Statement stmt = conn.createStatement()) {
                
                // 查询目录表的结构
                ResultSet tables = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table'");
                while (tables.next()) {
                    String tableName = tables.getString("name");
                    logger.debug("目录表中发现表: {}", tableName);
                    
                    // 尝试查询表内容，寻找名称字段
                    try {
                        // 常见的字段名：Name, TABLE_NAME, LAYER_NAME 等
                        String[] nameFields = {"Name", "TABLE_NAME", "LAYER_NAME", "name", "table_name", "layer_name"};
                        String[] idFields = {"ID", "OBJECTID", "id", "objectid"};
                        
                        for (String nameField : nameFields) {
                            try {
                                ResultSet rs = stmt.executeQuery("SELECT * FROM \"" + tableName + "\" LIMIT 10");
                                
                                // 检查是否有名称字段
                                boolean hasNameField = false;
                                boolean hasIdField = false;
                                
                                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                                    String columnName = rs.getMetaData().getColumnName(i);
                                    if (columnName.equalsIgnoreCase(nameField)) {
                                        hasNameField = true;
                                    }
                                    for (String idField : idFields) {
                                        if (columnName.equalsIgnoreCase(idField)) {
                                            hasIdField = true;
                                        }
                                    }
                                }
                                
                                if (hasNameField) {
                                    rs = stmt.executeQuery("SELECT * FROM \"" + tableName + "\"");
                                    while (rs.next()) {
                                        try {
                                            String layerName = rs.getString(nameField);
                                            if (layerName != null && !layerName.trim().isEmpty()) {
                                                // 如果有ID字段，用ID作为键，否则用行号
                                                String key = hasIdField ? "a" + String.format("%08d", rs.getInt(idFields[0])) : 
                                                           "a" + String.format("%08d", rs.getRow());
                                                tableNameMap.put(key, layerName.trim());
                                                logger.debug("映射: {} -> {}", key, layerName.trim());
                                            }
                                        } catch (Exception e) {
                                            logger.debug("读取行数据失败: {}", e.getMessage());
                                        }
                                    }
                                    break; // 找到有效的名称字段就退出
                                }
                                rs.close();
                            } catch (Exception e) {
                                logger.debug("查询字段 {} 失败: {}", nameField, e.getMessage());
                            }
                        }
                    } catch (Exception e) {
                        logger.debug("查询表 {} 内容失败: {}", tableName, e.getMessage());
                    }
                }
                tables.close();
                
            }
        } catch (Exception e) {
            logger.warn("读取 GDB 目录失败: {} - 将使用表ID作为图层名称", e.getMessage());
            if (logger.isDebugEnabled()) {
                logger.debug("读取 GDB 目录异常详情", e);
            }
        }
        
        return tableNameMap;
    }
    
    /**
     * 基本的 Shapefile 解析
     */
    private List<LayerInfo> parseShapefileBasic(String shpPath) throws IOException {
        logger.info("解析 Shapefile: {}", shpPath);
        
        List<LayerInfo> layers = new ArrayList<>();
        
        try (FileChannel channel = FileChannel.open(Paths.get(shpPath))) {
            LayerInfo layerInfo = new LayerInfo();
            
            // 读取 Shapefile 头文件（100字节）
            ByteBuffer buffer = ByteBuffer.allocate(100);
            channel.read(buffer);
            buffer.flip();
            
            // 检查文件代码（前4字节应该是 9994）
            buffer.order(ByteOrder.BIG_ENDIAN);
            int fileCode = buffer.getInt();
            if (fileCode != 9994) {
                throw new IOException("无效的 Shapefile 格式");
            }
            
            // 跳过未使用的字段
            buffer.position(24);
            
            // 读取文件长度（以16位字为单位）
            int fileLength = buffer.getInt() * 2; // 转换为字节
            
            // 读取版本和形状类型
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.getInt(); // version (未使用，但需要读取)
            int shapeType = buffer.getInt();
            
            // 设置图层信息
            String fileName = Paths.get(shpPath).getFileName().toString();
            String layerName = fileName.substring(0, fileName.lastIndexOf('.'));
            layerInfo.setLayerName(layerName);
            layerInfo.setFilePath(shpPath);
            layerInfo.setGeometryType(getShapeTypeName(shapeType));
            
            // 估算要素数量（基于文件大小的粗略计算）
            int estimatedFeatureCount = Math.max(1, (fileLength - 100) / 50); // 粗略估计
            layerInfo.setFeatureCount(estimatedFeatureCount);
            
            // 读取边界框
            double minX = buffer.getDouble();
            double minY = buffer.getDouble();
            double maxX = buffer.getDouble();
            double maxY = buffer.getDouble();
            
            String bbox = String.format("%.6f,%.6f,%.6f,%.6f", minX, minY, maxX, maxY);
            layerInfo.setBbox(bbox);
            layerInfo.setTotalArea(0.0);
            
            layers.add(layerInfo);
            logger.info("解析 Shapefile 完成: {} - 估计 {} 个要素", layerName, estimatedFeatureCount);
            
        } catch (Exception e) {
            logger.error("解析 Shapefile 失败: {}", e.getMessage(), e);
            throw new IOException("解析 Shapefile 失败: " + e.getMessage(), e);
        }
        
        return layers;
    }
    
    /**
     * 获取 Shapefile 几何类型名称
     */
    private String getShapeTypeName(int shapeType) {
        switch (shapeType) {
            case 0: return "Null Shape";
            case 1: return "Point";
            case 3: return "Polyline";
            case 5: return "Polygon";
            case 8: return "MultiPoint";
            case 11: return "PointZ";
            case 13: return "PolylineZ";
            case 15: return "PolygonZ";
            case 18: return "MultiPointZ";
            case 21: return "PointM";
            case 23: return "PolylineM";
            case 25: return "PolygonM";
            case 28: return "MultiPointM";
            case 31: return "MultiPatch";
            default: return "Unknown (" + shapeType + ")";
        }
    }
    
    /**
     * 图层元数据内部类
     */
    private static class LayerMetadata {
        String name;
        int featureCount;
        String geometryType;
    }
}
