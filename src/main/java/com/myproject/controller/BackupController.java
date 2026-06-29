package com.myproject.controller;

import com.myproject.entity.BackupRequest;
import com.myproject.entity.Result;
import com.myproject.service.BackupService;
import com.myproject.util.CurreetHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/backups")
public class BackupController {
    private final BackupService backupService;

    public BackupController(BackupService backupService) {
        this.backupService = backupService;
    }

    @GetMapping
    public Result list() {
        requireAdmin();
        return Result.success(backupService.listBackups());
    }

    @PostMapping
    public Result create(@RequestBody(required = false) BackupRequest request) {
        requireAdmin();
        return Result.success(backupService.createBackup(request));
    }

    @PostMapping("/{fileName}/restore")
    public Result restore(@PathVariable String fileName) {
        requireAdmin();
        int count = backupService.restore(fileName);
        return Result.success("恢复完成，执行SQL语句数: " + count);
    }

    private void requireAdmin() {
        if (!"ADMIN".equals(CurreetHolder.getCurrentUserType())) {
            throw new SecurityException("只有管理员可以执行备份恢复");
        }
    }
}
