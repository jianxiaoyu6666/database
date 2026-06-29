package com.myproject;

import com.myproject.service.BackupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

/**
 * 测试备份恢复功能（绕过 HTTP/TokenFilter，直接调 Service）
 */
@SpringBootTest
public class testBackupRestore {

    @Autowired
    private BackupService backupService;

    @Test
    void testFullBackup() throws Exception {
        System.out.println("===== 测试全量备份 =====");
        Map<String, Object> result = backupService.fullBackup();
        System.out.println("文件名: " + result.get("fileName"));
        System.out.println("路径: " + result.get("filePath"));
        System.out.println("大小: " + result.get("fileSize") + " bytes");
        System.out.println("创建时间: " + result.get("createdAt"));
        System.out.println("备份成功！");
    }

    @Test
    void testListFiles() throws Exception {
        System.out.println("===== 备份文件列表 =====");
        List<Map<String, Object>> files = backupService.listBackupFiles();
        for (Map<String, Object> f : files) {
            System.out.println(f.get("fileName") + " | " + f.get("fileSize") + " bytes | " + f.get("lastModified"));
        }
        if (files.isEmpty()) {
            System.out.println("（暂无备份文件）");
        }
    }

    @Test
    void testBackupThenRestore() throws Exception {
        System.out.println("===== 备份 + 恢复 联合测试 =====");

        // 1. 备份
        Map<String, Object> info = backupService.fullBackup();
        String fileName = (String) info.get("fileName");
        System.out.println("备份完成: " + fileName);

        // 2. 恢复
        backupService.restore(fileName);
        System.out.println("恢复完成: " + fileName);
        System.out.println("备份恢复联合测试通过！");
    }
}
