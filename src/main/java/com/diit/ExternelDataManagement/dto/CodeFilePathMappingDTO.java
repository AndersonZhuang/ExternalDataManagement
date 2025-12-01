package com.diit.ExternelDataManagement.dto;

/**
 * Code和FilePath映射数据传输对象
 * 用于返回批量查询的code和filepath映射关系
 * 
 * @author Assistant
 * @since 2025-11-06
 */
public class CodeFilePathMappingDTO {

    private String code;
    private String filePath;

    public CodeFilePathMappingDTO() {
    }

    public CodeFilePathMappingDTO(String code, String filePath) {
        this.code = code;
        this.filePath = filePath;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public String toString() {
        return "CodeFilePathMappingDTO{" +
                "code='" + code + '\'' +
                ", filePath='" + filePath + '\'' +
                '}';
    }
}
