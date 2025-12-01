package com.diit.ExternelDataManagement.service.impl;

import com.diit.ExternelDataManagement.pojo.LayerInfo;
import com.diit.ExternelDataManagement.service.GeoSpatialParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 地理空间数据解析服务实现
 * 使用简化的纯 Java 解析器，支持 GDB 和 Shapefile
 * 
 * @author Assistant
 * @since 2025-11-05
 */
@Service
public class GeoSpatialParserImpl implements GeoSpatialParser {

    private static final Logger logger = LoggerFactory.getLogger(GeoSpatialParserImpl.class);
    
    private final SimpleGeoParser simpleGeoParser;
    
    // 构造函数注入
    public GeoSpatialParserImpl(SimpleGeoParser simpleGeoParser) {
        this.simpleGeoParser = simpleGeoParser;
    }

    @Override
    public List<LayerInfo> parseLayers(String filePath) {
        logger.info("开始解析地理空间数据文件: {}", filePath);
        List<LayerInfo> layerInfos = new ArrayList<>();

        try {
            // 使用简化的解析器
            layerInfos.addAll(simpleGeoParser.parseGeospatialFile(filePath));
            logger.info("文件解析完成，共解析了 {} 个图层", layerInfos.size());
        } catch (Exception e) {
            logger.error("解析文件时发生异常: {} - {}", filePath, e.getMessage(), e);
            // 不重新抛出异常，返回空列表，让调用方知道解析失败但没有数据
        }

        return layerInfos;
    }

    @Override
    public boolean canParse(String filePath) {
        String extension = getFileExtension(filePath).toLowerCase();
        return extension.equals("shp") || extension.equals("gdb");
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filePath) {
        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filePath.length() - 1) {
            return filePath.substring(lastDotIndex + 1);
        }
        return "";
    }
}