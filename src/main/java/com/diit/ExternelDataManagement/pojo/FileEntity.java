package com.diit.ExternelDataManagement.pojo;

public class FileEntity {

    private String id;
    private String receiveCode;
    private String filePath;
    private String dataType;
    private String fileType;
    private String bbox;
    private String dataSize;
    private Integer totalObjectNum;
    private Double totalArea;
    private String layerName;

    public FileEntity() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getReceiveCode() {
        return receiveCode;
    }

    public void setReceiveCode(String receiveCode) {
        this.receiveCode = receiveCode;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getBbox() {
        return bbox;
    }

    public void setBbox(String bbox) {
        this.bbox = bbox;
    }

    public String getDataSize() {
        return dataSize;
    }

    public void setDataSize(String dataSize) {
        this.dataSize = dataSize;
    }

    public Integer getTotalObjectNum() {
        return totalObjectNum;
    }

    public void setTotalObjectNum(Integer totalObjectNum) {
        this.totalObjectNum = totalObjectNum;
    }

    public Double getTotalArea() {
        return totalArea;
    }

    public void setTotalArea(Double totalArea) {
        this.totalArea = totalArea;
    }

    public String getLayerName() {
        return layerName;
    }

    public void setLayerName(String layerName) {
        this.layerName = layerName;
    }

    @Override
    public String toString() {
        return "FileEntity{" +
                "id='" + id + '\'' +
                ", receiveCode='" + receiveCode + '\'' +
                ", filePath='" + filePath + '\'' +
                ", dataType='" + dataType + '\'' +
                ", fileType='" + fileType + '\'' +
                ", bbox='" + bbox + '\'' +
                ", dataSize='" + dataSize + '\'' +
                ", totalObjectNum=" + totalObjectNum +
                ", totalArea=" + totalArea +
                ", layerName='" + layerName + '\'' +
                '}';
    }
}