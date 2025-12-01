package com.diit.ExternelDataManagement.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 数据库初始化类
 * 在应用启动时自动执行建表SQL脚本
 * 
 * @author Assistant
 * @since 2025-01-XX
 */
@Component
@Order(1)
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseInitializer(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        logger.info("开始初始化数据库表...");
        
        try {
            // 初始化NAS表
            initNasTable();
            
            logger.info("数据库表初始化完成");
        } catch (Exception e) {
            logger.error("数据库表初始化失败", e);
            // 不抛出异常，避免影响应用启动
        }
    }

    /**
     * 初始化NAS表
     */
    private void initNasTable() {
        try {
            // 先获取当前数据库信息
            String currentDb = jdbcTemplate.queryForObject("SELECT current_database()", String.class);
            String currentSchema = jdbcTemplate.queryForObject("SELECT current_schema()", String.class);
            logger.info("=== 数据库初始化开始 ===");
            logger.info("当前数据库: {}, 当前Schema: {}", currentDb, currentSchema);
            
            // 检查表是否已存在（PostgreSQL中表名在information_schema中是小写的）
            // 使用LOWER函数确保大小写不敏感
            String checkTableSql = "SELECT COUNT(*) FROM information_schema.tables " +
                                  "WHERE table_schema = ? AND LOWER(table_name) = LOWER(?)";
            Integer count = jdbcTemplate.queryForObject(checkTableSql, Integer.class, currentSchema, "nas_info");
            
            boolean tableExists = count != null && count > 0;
            logger.info("检查nas_info表是否存在: count={}, exists={}", count, tableExists);
            
            // 如果表不存在，尝试创建
            if (!tableExists) {
                logger.info("nas_info表不存在，开始创建...");
                
                // 读取SQL脚本
                ClassPathResource resource = new ClassPathResource("sql/create_nas_table.sql");
                String sql = FileCopyUtils.copyToString(
                        new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)
                );
                logger.debug("读取到的SQL脚本内容:\n{}", sql);

                // 执行SQL脚本（按分号分割，逐条执行）
                String[] statements = sql.split(";");
                logger.info("SQL脚本分割后共 {} 条语句", statements.length);
                
                boolean createSuccess = false;
                int executedCount = 0;
                int skippedCount = 0;
                
                for (int i = 0; i < statements.length; i++) {
                    String statement = statements[i];
                    String trimmed = statement.trim();
                    
                    // 跳过空语句和注释
                    if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                        skippedCount++;
                        logger.debug("跳过语句 {}: {}", i + 1, trimmed.length() > 50 ? trimmed.substring(0, 50) + "..." : trimmed);
                        continue;
                    }
                    
                    executedCount++;
                    logger.info("准备执行语句 {}: {}", executedCount, trimmed.length() > 150 ? trimmed.substring(0, 150) + "..." : trimmed);
                    
                    try {
                        jdbcTemplate.execute(trimmed);
                        createSuccess = true;
                        logger.info("语句 {} 执行成功", executedCount);
                    } catch (Exception e) {
                        String errorMsg = e.getMessage() != null ? e.getMessage() : "";
                        String fullError = e.getClass().getName() + ": " + errorMsg;
                        logger.error("语句 {} 执行失败: {}", executedCount, fullError, e);
                        
                        // 忽略已存在的错误（表、约束已存在）
                        if (errorMsg.toLowerCase().contains("already exists") || 
                            errorMsg.toLowerCase().contains("duplicate key") ||
                            errorMsg.toLowerCase().contains("relation") && errorMsg.toLowerCase().contains("already exists")) {
                            logger.warn("对象已存在，视为成功: {}", errorMsg);
                            createSuccess = true; // 表已存在也算成功
                        } else {
                            // 记录详细错误但不抛出，继续尝试其他语句
                            logger.error("语句 {} 执行失败，错误详情: {}", executedCount, fullError);
                            // 不抛出异常，继续执行其他语句
                        }
                    }
                }
                
                logger.info("SQL执行统计: 总语句数={}, 执行数={}, 跳过数={}, 成功={}", 
                    statements.length, executedCount, skippedCount, createSuccess);
                
                if (createSuccess) {
                    // 等待一下，确保事务提交
                    Thread.sleep(200);
                    
                    // 创建后立即验证表是否真的存在
                    Integer verifyCount = jdbcTemplate.queryForObject(checkTableSql, Integer.class, currentSchema, "nas_info");
                    if (verifyCount != null && verifyCount > 0) {
                        logger.info("nas_info表创建成功并验证通过");
                        verifyTableStructure();
                    } else {
                        logger.error("nas_info表创建失败：表不存在！请检查数据库连接和权限");
                        // 列出所有表，帮助调试
                        listAllTables(currentSchema);
                    }
                } else {
                    logger.error("nas_info表创建失败：没有成功执行任何SQL语句");
                    logger.error("请检查SQL脚本内容和数据库权限");
                    // 尝试直接执行建表语句
                    tryDirectCreate();
                }
            } else {
                logger.info("nas_info表已存在，跳过创建");
                // 验证表结构
                verifyTableStructure();
            }
            
            // 添加注释（表已存在时也可以执行，使用DO块来避免错误）
            addTableComments();
            
            logger.info("=== 数据库初始化完成 ===");
            
        } catch (Exception e) {
            logger.error("创建nas_info表失败", e);
            // 不抛出异常，避免影响应用启动
            logger.warn("nas_info表初始化失败，但应用将继续启动");
        }
    }
    
    /**
     * 列出所有表（用于调试）
     */
    private void listAllTables(String schema) {
        try {
            String listTablesSql = "SELECT table_name FROM information_schema.tables " +
                                  "WHERE table_schema = ? ORDER BY table_name";
            logger.info("当前Schema '{}' 中的所有表:", schema);
            jdbcTemplate.query(listTablesSql, (rs) -> {
                logger.info("  - {}", rs.getString("table_name"));
            }, schema);
        } catch (Exception e) {
            logger.warn("列出表时出错: {}", e.getMessage());
        }
    }
    
    /**
     * 尝试直接创建表（备用方案）
     */
    private void tryDirectCreate() {
        try {
            logger.info("尝试使用直接SQL创建表...");
            String directCreateSql = 
                "CREATE TABLE IF NOT EXISTS nas_info (" +
                "    ID VARCHAR(50) PRIMARY KEY, " +
                "    NAS_NAME VARCHAR(100) NOT NULL, " +
                "    NAS_IP JSONB NOT NULL, " +
                "    CONSTRAINT uk_nas_name UNIQUE (NAS_NAME)" +
                ")";
            
            logger.info("执行直接创建SQL: {}", directCreateSql);
            jdbcTemplate.execute(directCreateSql);
            logger.info("直接创建SQL执行成功");
            
            // 验证表是否存在
            Thread.sleep(200);
            String currentSchema = jdbcTemplate.queryForObject("SELECT current_schema()", String.class);
            String checkTableSql = "SELECT COUNT(*) FROM information_schema.tables " +
                                  "WHERE table_schema = ? AND LOWER(table_name) = LOWER(?)";
            Integer verifyCount = jdbcTemplate.queryForObject(checkTableSql, Integer.class, currentSchema, "nas_info");
            
            if (verifyCount != null && verifyCount > 0) {
                logger.info("直接创建成功，表已存在");
                verifyTableStructure();
            } else {
                logger.error("直接创建失败，表仍然不存在");
            }
        } catch (Exception e) {
            logger.error("直接创建表也失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 验证表结构
     */
    private void verifyTableStructure() {
        try {
            String verifySql = "SELECT column_name, data_type, character_maximum_length " +
                              "FROM information_schema.columns " +
                              "WHERE table_schema = current_schema() AND table_name = 'nas_info' " +
                              "ORDER BY ordinal_position";
            
            jdbcTemplate.query(verifySql, (rs) -> {
                logger.info("表结构验证 - 列: {}, 类型: {}, 长度: {}", 
                    rs.getString("column_name"), 
                    rs.getString("data_type"),
                    rs.getInt("character_maximum_length"));
            });
            
            // 检查约束
            String constraintSql = "SELECT constraint_name, constraint_type " +
                                  "FROM information_schema.table_constraints " +
                                  "WHERE table_schema = current_schema() AND table_name = 'nas_info'";
            
            jdbcTemplate.query(constraintSql, (rs) -> {
                logger.info("表约束验证 - 约束名: {}, 类型: {}", 
                    rs.getString("constraint_name"), 
                    rs.getString("constraint_type"));
            });
            
        } catch (Exception e) {
            logger.warn("验证表结构时出错: {}", e.getMessage());
        }
    }
    
    /**
     * 添加表注释
     */
    private void addTableComments() {
        try {
            // 使用DO块来安全地添加注释，如果表不存在则跳过
            String commentSql = 
                "DO $$ " +
                "BEGIN " +
                "  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'nas_info') THEN " +
                "    COMMENT ON TABLE nas_info IS 'NAS存储设备信息表'; " +
                "    COMMENT ON COLUMN nas_info.ID IS 'NAS记录ID'; " +
                "    COMMENT ON COLUMN nas_info.NAS_NAME IS 'NAS名称'; " +
                "    COMMENT ON COLUMN nas_info.NAS_IP IS 'NAS地址信息（JSON格式）'; " +
                "  END IF; " +
                "END $$;";
            
            jdbcTemplate.execute(commentSql);
            logger.debug("nas_info表注释添加成功");
        } catch (Exception e) {
            logger.debug("添加nas_info表注释时出现警告（可忽略）: {}", e.getMessage());
        }
    }
}

