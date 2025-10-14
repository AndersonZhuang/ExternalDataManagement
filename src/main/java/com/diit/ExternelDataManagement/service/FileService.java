package com.diit.ExternelDataManagement.service;

import com.diit.ExternelDataManagement.pojo.FileEntity;
import java.util.List;

public interface FileService {

    List<FileEntity> parseAndSaveFiles(String receiveCode);
}