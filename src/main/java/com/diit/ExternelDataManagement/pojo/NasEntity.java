package com.diit.ExternelDataManagement.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * NAS实体类
 * 
 * @author Assistant
 * @since 2025-01-XX
 */
public class NasEntity {

    private String id;
    private String nasName;
    
    /**
     * NAS地址信息，JSON格式
     * 例如: {"ip": "192.168.1.100", "port": 8080} 或 {"domain": "nas.example.com"}
     */
    private String nasIp;

    public NasEntity() {
    }

    public NasEntity(String id, String nasName, String nasIp) {
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

    public String getNasIp() {
        return nasIp;
    }

    public void setNasIp(String nasIp) {
        this.nasIp = nasIp;
    }

    @Override
    public String toString() {
        return "NasEntity{" +
                "id='" + id + '\'' +
                ", nasName='" + nasName + '\'' +
                ", nasIp='" + nasIp + '\'' +
                '}';
    }
}

