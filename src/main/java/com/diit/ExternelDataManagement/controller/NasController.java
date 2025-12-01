package com.diit.ExternelDataManagement.controller;

import com.diit.ExternelDataManagement.common.APIResponse;
import com.diit.ExternelDataManagement.dto.NasDTO;
import com.diit.ExternelDataManagement.pojo.NasEntity;
import com.diit.ExternelDataManagement.service.NasService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * NAS管理控制器
 * 
 * @author Assistant
 * @since 2025-01-XX
 */
@RestController
@RequestMapping("/api/nas")
@Tag(name = "NAS管理", description = "NAS存储设备管理接口")
public class NasController {

    @Autowired
    private NasService nasService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping
    @Operation(summary = "创建NAS记录", description = "创建新的NAS存储设备记录，nasIp字段为JSON对象格式")
    public APIResponse<NasDTO> createNas(
            @Parameter(description = "NAS对象，nasIp为JSON格式", required = true)
            @RequestBody NasDTO nasDTO) {
        NasEntity nasEntity = convertToEntity(nasDTO);
        NasEntity created = nasService.createNas(nasEntity);
        NasDTO result = convertToDTO(created);
        return APIResponse.ok("NAS记录创建成功", result);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除NAS记录", description = "根据ID删除NAS存储设备记录")
    public APIResponse<String> deleteNas(
            @Parameter(description = "NAS记录ID", required = true, example = "NAS_20250101_120000_ABCD1234")
            @PathVariable String id) {
        boolean deleted = nasService.deleteNas(id);
        if (deleted) {
            return APIResponse.ok("NAS记录删除成功");
        } else {
            return APIResponse.failed("NAS记录删除失败");
        }
    }

    @PutMapping
    @Operation(summary = "更新NAS记录", description = "更新NAS存储设备记录，nasIp字段为JSON对象格式")
    public APIResponse<NasDTO> updateNas(
            @Parameter(description = "NAS对象，nasIp为JSON格式", required = true)
            @RequestBody NasDTO nasDTO) {
        NasEntity nasEntity = convertToEntity(nasDTO);
        NasEntity updated = nasService.updateNas(nasEntity);
        NasDTO result = convertToDTO(updated);
        return APIResponse.ok("NAS记录更新成功", result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询NAS记录", description = "根据ID查询NAS存储设备记录")
    public APIResponse<NasDTO> getNasById(
            @Parameter(description = "NAS记录ID", required = true, example = "NAS_20250101_120000_ABCD1234")
            @PathVariable String id) {
        NasEntity nasEntity = nasService.getNasById(id);
        NasDTO result = convertToDTO(nasEntity);
        return APIResponse.ok(result);
    }

    @GetMapping
    @Operation(summary = "查询所有NAS记录", description = "查询所有NAS存储设备记录")
    public APIResponse<List<NasDTO>> getAllNas() {
        List<NasEntity> nasList = nasService.getAllNas();
        List<NasDTO> result = nasList.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return APIResponse.ok(result);
    }
    
    /**
     * 将DTO转换为Entity
     */
    private NasEntity convertToEntity(NasDTO dto) {
        NasEntity entity = new NasEntity();
        entity.setId(dto.getId());
        entity.setNasName(dto.getNasName());
        
        // 将Object类型的nasIp转换为JSON字符串
        if (dto.getNasIp() != null) {
            try {
                String jsonString = objectMapper.writeValueAsString(dto.getNasIp());
                entity.setNasIp(jsonString);
            } catch (Exception e) {
                throw new IllegalArgumentException("nasIp字段必须是有效的JSON格式: " + e.getMessage());
            }
        }
        
        return entity;
    }
    
    /**
     * 将Entity转换为DTO
     */
    private NasDTO convertToDTO(NasEntity entity) {
        NasDTO dto = new NasDTO();
        dto.setId(entity.getId());
        dto.setNasName(entity.getNasName());
        
        // 将JSON字符串转换为Object
        if (entity.getNasIp() != null && !entity.getNasIp().trim().isEmpty()) {
            try {
                Object jsonObject = objectMapper.readValue(entity.getNasIp(), Object.class);
                dto.setNasIp(jsonObject);
            } catch (Exception e) {
                // 如果解析失败，保持为字符串
                dto.setNasIp(entity.getNasIp());
            }
        }
        
        return dto;
    }
}

