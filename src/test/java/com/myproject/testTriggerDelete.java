package com.myproject;

import com.myproject.entity.User;
import com.myproject.entity.audit_log;
import com.myproject.mapper.LogMapper;
import com.myproject.service.UserService;
import com.myproject.util.CurreetHolder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * 测试 delete() → @Log(DELETE) → 触发器写 audit_log
 *
 * 前提：03_triggers.sql 的 trg_user_audit_delete 已执行
 * 注意：会真实删除测试用户，每次运行需要新的用户名
 */
@SpringBootTest
public class testTriggerDelete {

    @Autowired
    private UserService userService;
    @Autowired
    private LogMapper logMapper;

    @Test
    void deleteUserAndCheckAuditLog() {
        // ===== 步骤1: 先注册一个测试用户 =====
        User u = new User();
        u.setUsername("test_delete_user_" + System.currentTimeMillis() % 100000);
        u.setPassword("123456");
        u.setRealName("删除测试");
        u.setIdNumber("11010119950302002X");
        u.setPhone("13800000003");
        u.setUserType("USER");
        userService.register(u);

        Long userId = u.getId();
        System.out.println("注册成功，用户ID: " + userId);

        // ===== 步骤2: 模拟已登录 =====
        CurreetHolder.setCurrentId(userId);
        CurreetHolder.setCurrentUsertype("USER");

        // ===== 步骤3: 调用 delete =====
        userService.delete(userId);
        System.out.println("删除成功，用户ID: " + userId);

        // ===== 步骤4: 查询 audit_log 验证 =====
        List<audit_log> logs = logMapper.list("user", "DELETE", null, null, null);
        System.out.println("===== user 表 DELETE 审计日志 =====");
        for (audit_log log : logs) {
            System.out.println("  id=" + log.getId()
                    + " | recordId=" + log.getRecordId()
                    + " | operatedBy=" + log.getOperatedBy()
                    + " | oldValue=" + log.getOldValue()
                    + " | operatedAt=" + log.getOperatedAt());
        }

        if (logs.isEmpty()) {
            System.out.println("⚠️ 未找到 DELETE 审计日志！请检查：");
            System.out.println("  1. 03_triggers.sql 是否已执行？");
            System.out.println("  2. trg_user_audit_delete 触发器是否存在？");
            System.out.println("  3. @Log 注解是否生效？");
        }

        // ===== 清理 ThreadLocal =====
        CurreetHolder.remove();
    }
}
