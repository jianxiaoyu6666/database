package com.myproject.service;

import java.util.List;
import java.util.Map;

public interface BackupService {

    /** 全量备份，返回备份文件信息 */
    Map<String, Object> fullBackup() throws Exception;

    /** 列出所有备份文件 */
    List<Map<String, Object>> listBackupFiles() throws Exception;

    /** 从指定备份文件恢复数据库 */
    void restore(String fileName) throws Exception;
}
