package com.diit.ExternelDataManagement.service;

import com.diit.ExternelDataManagement.pojo.LayerInfo;

import java.util.List;

/**
 * 地理空间数据解析服务接口
 * 用于解析shp、gdb、mdb等地理数据文件
 */
public interface GeoSpatialParser {

    /**
     * 解析地理数据文件，提取图层信息
     * @param filePath 文件路径
     * @return 图层信息列表
     */
    List<LayerInfo> parseLayers(String filePath);

    /**
     * 检查文件是否支持解析
     * @param filePath 文件路径
     * @return true表示支持，false表示不支持
     */
    boolean canParse(String filePath);
}

