package com.myproject.service;

import com.myproject.entity.BackupInfo;
import com.myproject.entity.BackupRequest;

import java.util.List;

public interface BackupService {
    BackupInfo createBackup(BackupRequest request);

    List<BackupInfo> listBackups();

    int restore(String fileName);
}
