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
 * 测试 update() → @Log(UPDATE) → 触发器写 audit_log
 *
 * 前提：03_triggers.sql 的 trg_user_audit_update 已执行
 * 注意：本测试需要在数据库中有可用的测试用户
 */
@SpringBootTest
public class testTriggerUpdate {

    @Autowired
    private UserService userService;
    @Autowired
    private LogMapper logMapper;

    @Test
    void updateUserAndCheckAuditLog() {
        // ===== 步骤1: 先注册一个测试用户 =====
        User u = new User();
        u.setUsername("test_update_user");
        u.setPassword("123456");
        u.setRealName("更新测试");
        u.setIdNumber("11010119950203001X");
        u.setPhone("13800000001");
        u.setUserType("USER");
        userService.register(u);

        // ===== 步骤2: 模拟已登录（设置 CurreetHolder）=====
        // 需要从数据库查出刚注册用户的 id
        // 由于 register 用了 @Options(useGeneratedKeys = true)，user.id 会回填
        Long userId = u.getId();
        System.out.println("注册成功，用户ID: " + userId);

        CurreetHolder.setCurrentId(userId);
        CurreetHolder.setCurrentUsertype("USER");

        // ===== 步骤3: 调用 update =====
        userService.update("test_update_new", "654321", "13800000002", "new@test.com");

        // ===== 步骤4: 查询 audit_log 验证 =====
        List<audit_log> logs = logMapper.list("user", "UPDATE", null, null, null);
        System.out.println("===== user 表 UPDATE 审计日志 =====");
        for (audit_log log : logs) {
            System.out.println("  id=" + log.getId()
                    + " | recordId=" + log.getRecordId()
                    + " | operatedBy=" + log.getOperatedBy()
                    + " | oldValue=" + log.getOldValue()
                    + " | newValue=" + log.getNewValue()
                    + " | operatedAt=" + log.getOperatedAt());
        }

        if (logs.isEmpty()) {
            System.out.println("⚠️ 未找到 UPDATE 审计日志！请检查：");
            System.out.println("  1. 03_triggers.sql 是否已执行？");
            System.out.println("  2. trg_user_audit_update 触发器是否存在？");
            System.out.println("  3. @Log 注解是否生效？（检查 LogAspect 是否拦截）");
        }

        // ===== 清理 ThreadLocal =====
        CurreetHolder.remove();
    }
}
