-- NAS信息表
-- 用于存储NAS存储设备的基本信息
-- PostgreSQL语法

-- 创建表（如果不存在）
CREATE TABLE IF NOT EXISTS nas_info (
    ID VARCHAR(50) PRIMARY KEY,
    NAS_NAME VARCHAR(100) NOT NULL,
    NAS_IP JSONB NOT NULL,
    CONSTRAINT uk_nas_name UNIQUE (NAS_NAME)
);

