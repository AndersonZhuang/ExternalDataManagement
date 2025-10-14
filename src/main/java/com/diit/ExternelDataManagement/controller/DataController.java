package com.diit.ExternelDataManagement.controller;

import com.diit.ExternelDataManagement.common.APIResponse;
import com.diit.ExternelDataManagement.pojo.DataEntity;
import com.diit.ExternelDataManagement.pojo.FileEntity;
import com.diit.ExternelDataManagement.service.DataService;
import com.diit.ExternelDataManagement.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/data")
@Tag(name = "数据管理", description = "外部数据包接收处理接口")
public class DataController {

    @Autowired
    private DataService dataService;

    @Autowired
    private FileService fileService;

    @PostMapping("/receive/{id}")
    @Operation(summary = "处理数据接收", description = "根据ID处理外部数据包接收，生成接收编码并更新状态")
    public APIResponse<DataEntity> processDataById(
            @Parameter(description = "数据包ID", required = true, example = "data-001")
            @PathVariable String id) {
        DataEntity processedData = dataService.processDataById(id);
        return APIResponse.ok(processedData);
    }

    @PostMapping("/parse/{receiveCode}")
    @Operation(summary = "解析文件目录", description = "根据接收编码查询文件路径，解析目录下所有文件并存入数据库")
    public APIResponse<List<FileEntity>> parseFilesByReceiveCode(
            @Parameter(description = "接收编码", required = true, example = "REC_20241201_143022_A1B2C3D4")
            @PathVariable String receiveCode) {
        List<FileEntity> parsedFiles = fileService.parseAndSaveFiles(receiveCode);
        return APIResponse.ok(parsedFiles);
    }
}