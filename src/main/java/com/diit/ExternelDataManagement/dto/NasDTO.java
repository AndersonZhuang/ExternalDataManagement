package com.diit.ExternelDataManagement.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * NAS数据传输对象
 * 用于API接口的请求和响应
 * 
 * @author Assistant
 * @since 2025-01-XX
 */
@Schema(description = "NAS存储设备信息")
public class NasDTO {

    @Schema(description = "NAS记录ID", example = "NAS_20250101_120000_ABCD1234")
    private String id;

    @Schema(description = "NAS名称", example = "主存储服务器")
    private String nasName;

    @Schema(description = "NAS地址信息（JSON对象）", example = "{\"ip\": \"192.168.1.100\", \"port\": 8080}")
    private Object nasIp;

    public NasDTO() {
    }

    public NasDTO(String id, String nasName, Object nasIp) {
        this.id = id;
        this.nasName = nasName;
        this.nasIp = nasIp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNasName() {
        return nasName;
    }

    public void setNasName(String nasName) {
        this.nasName = nasName;
    }

    public Object getNasIp() {
        return nasIp;
    }

    public void setNasIp(Object nasIp) {
        this.nasIp = nasIp;
    }

    @Override
    public String toString() {
        return "NasDTO{" +
                "id='" + id + '\'' +
                ", nasName='" + nasName + '\'' +
                ", nasIp=" + nasIp +
                '}';
    }
}

