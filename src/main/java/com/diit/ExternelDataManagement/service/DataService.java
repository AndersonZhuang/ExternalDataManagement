package com.diit.ExternelDataManagement.service;

import com.diit.ExternelDataManagement.pojo.DataEntity;
import com.diit.ExternelDataManagement.pojo.SyncStatusResult;

public interface DataService {

    DataEntity processDataById(String id);

    DataEntity restartQualityCheck(String id);

    SyncStatusResult syncQualityCheckStatus();
}