package com.myproject.controller;

import com.myproject.entity.Result;
import com.myproject.service.BackupService;
import com.myproject.util.CurreetHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/backup")
public class BackupController {

    @Autowired
    private BackupService backupService;

    /**
     * 手动触发全量备份
     * POST /api/backup/full
     */
    @PostMapping("/full")
    public Result fullBackup() {
        if (!"ADMIN".equals(CurreetHolder.getCurrentUserType())) {
            return Result.error("只有管理员可以执行备份操作");
        }
        try {
            Map<String, Object> info = backupService.fullBackup();
            return Result.success(info);
        } catch (Exception e) {
            return Result.error("备份失败: " + e.getMessage());
        }
    }

    /**
     * 列出所有备份文件
     * GET /api/backup/files
     */
    @GetMapping("/files")
    public Result listFiles() {
        if (!"ADMIN".equals(CurreetHolder.getCurrentUserType())) {
            return Result.error("只有管理员可以查看备份列表");
        }
        try {
            List<Map<String, Object>> files = backupService.listBackupFiles();
            return Result.success(files);
        } catch (Exception e) {
            return Result.error("获取备份列表失败: " + e.getMessage());
        }
    }

    /**
     * 恢复数据库（需要确认参数防止误操作）
     * POST /api/backup/restore?fileName=xxx.sql&confirm=yes
     */
    @PostMapping("/restore")
    public Result restore(@RequestParam String fileName,
                          @RequestParam(defaultValue = "no") String confirm) {
        if (!"ADMIN".equals(CurreetHolder.getCurrentUserType())) {
            return Result.error("只有管理员可以执行恢复操作");
        }
        if (!"yes".equals(confirm)) {
            return Result.error("恢复操作危险，请传 confirm=yes 确认");
        }
        try {
            backupService.restore(fileName);
            return Result.success("恢复成功，数据库已从 " + fileName + " 还原");
        } catch (Exception e) {
            return Result.error("恢复失败: " + e.getMessage());
        }
    }
}
