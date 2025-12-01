package com.diit.ExternelDataManagement.pojo;

/**
 * 图层信息实体类
 */
public class LayerInfo {
    
    private String layerName;
    private int featureCount;
    private String geometryType;
    private String bbox;
    private double totalArea;
    private String filePath; // 图层对应的文件路径（主要用于 GDB 中每个图层对应的 .gdbtable 文件）
    
    public LayerInfo() {
    }
    
    public LayerInfo(String layerName, int featureCount) {
        this.layerName = layerName;
        this.featureCount = featureCount;
    }
    
    public String getLayerName() {
        return layerName;
    }
    
    public void setLayerName(String layerName) {
        this.layerName = layerName;
    }
    
    public int getFeatureCount() {
        return featureCount;
    }
    
    public void setFeatureCount(int featureCount) {
        this.featureCount = featureCount;
    }
    
    public String getGeometryType() {
        return geometryType;
    }
    
    public void setGeometryType(String geometryType) {
        this.geometryType = geometryType;
    }
    
    public String getBbox() {
        return bbox;
    }
    
    public void setBbox(String bbox) {
        this.bbox = bbox;
    }
    
    public double getTotalArea() {
        return totalArea;
    }
    
    public void setTotalArea(double totalArea) {
        this.totalArea = totalArea;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    @Override
    public String toString() {
        return "LayerInfo{" +
                "layerName='" + layerName + '\'' +
                ", featureCount=" + featureCount +
                ", geometryType='" + geometryType + '\'' +
                ", bbox='" + bbox + '\'' +
                ", totalArea=" + totalArea +
                '}';
    }
}

